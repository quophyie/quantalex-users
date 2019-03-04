package com.quantal.exchange.users.config.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantal.exchange.users.services.api.ApiGatewayService;
import com.quantal.exchange.users.services.api.AuthorizationApiService;
import com.quantal.exchange.users.services.api.EmailApiService;
import com.quantal.exchange.users.services.api.GiphyApiService;
import com.quantal.javashared.aspects.RetrofitRequiredHeadersEnforcerAspectJAspect;
import com.quantal.javashared.constants.CommonConstants;
import com.quantal.javashared.constants.Events;
import com.quantal.javashared.dto.LogEvent;
import com.quantal.javashared.dto.LoggerConfig;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.logger.QuantalLoggerFactory;
import com.quantal.javashared.util.CommonUtils;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.aspectj.lang.Aspects;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.async.TraceableExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import retrofit2.Retrofit;
import retrofit2.adapter.java8.Java8CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.quantal.javashared.constants.CommonConstants.EVENT_HEADER_KEY;
import static com.quantal.javashared.constants.CommonConstants.EVENT_KEY;
import static com.quantal.javashared.constants.CommonConstants.TRACE_ID_HEADER_KEY;
import static com.quantal.javashared.constants.CommonConstants.TRACE_ID_MDC_KEY;

/**
 * Created by dman on 14/03/2017.
 *
 * This class should contain the Retrofit API interfaces
 */

@Configuration
public class ApiConfig// implements AsyncConfigurer
{

    //private XLogger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Environment env;


    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    private Tracer tracer;
    @Autowired
    private SpanNamer spanNamer;
    @Autowired
    private TraceKeys traceKeys;

    @Value("${app.service-endpoints-not-requiring-mandatory-propagated-headers-uri-patterns}")
    private List<String> serviceEndpointsNotRequiringMandatoryPropagatedHeadersPatterns;


    private QuantalLogger logger;

    @Autowired
    public ApiConfig(Environment env, LoggerConfig loggerConfig){
        this.env = env;
        logger = QuantalLoggerFactory.getLogzioLogger(ApiConfig.class, loggerConfig);
    }


    private String getRequestBody(Request request, Buffer buffer ){
        if (request.body() != null) {
            try {
                request.body().writeTo(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return buffer.readUtf8();
        }
        return  null;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        ExecutorService executorService = getAsyncExecutor();
        //Dispatcher dispatcher = new Dispatcher(executorService);
        Dispatcher dispatcher = new Dispatcher();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        //builder.dispatcher(dispatcher);

        Map<String, String> previous = MDC.getCopyOfContextMap();
        /*builder.interceptors()
                .add(0, chain -> {
                    Request original = chain.request();

                    // Request customization: add request headers
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("MyTestHeader", "auth-value"); // <-- this is the important line

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                });*/

        builder.interceptors().add(chain -> {

            String requestBody = null;
            Request request = chain.request();
            Request newRequest;


            MDC.put(EVENT_KEY, request.header(EVENT_HEADER_KEY));
            MDC.put(TRACE_ID_MDC_KEY, request.header(TRACE_ID_HEADER_KEY));
            LogEvent event = new LogEvent(request.header(CommonConstants.EVENT_HEADER_KEY));

            boolean bEndpointRequiresMandatoryPropagatedHeaders = CommonUtils
                    .isMandatoryPropagatedHeadersRequiredToCallEndpoint(request.url().toString(),
                            serviceEndpointsNotRequiringMandatoryPropagatedHeadersPatterns);

            final boolean bAllRequiredHeadersFound = request
                    .headers()
                    .names()
                    .stream()
                    .filter(name -> name.equalsIgnoreCase(EVENT_HEADER_KEY) || name.equalsIgnoreCase(TRACE_ID_HEADER_KEY))
                    .count() == 2;

            if(!bAllRequiredHeadersFound && bEndpointRequiresMandatoryPropagatedHeaders){
                String msg = String.format("The required request headers %s and %s were not found. " +
                        "Please make sure that your api interface method " +
                        "has headers %s and %s ",
                        EVENT_HEADER_KEY, TRACE_ID_HEADER_KEY, EVENT_HEADER_KEY, TRACE_ID_HEADER_KEY);
                RuntimeException exception =  new IllegalStateException(msg);
                throw exception;

            }

            final Buffer buffer = new Buffer();
           /* if (request.body() != null) {
                request.body().writeTo(buffer);
                requestBody = buffer.readUtf8();
            }*/
            long t1 = System.nanoTime();
            //newRequest = request.newBuilder().addHeader(TRACE_ID_HEADER_KEY, MDC.get("X-B3-TraceId")).build();
            newRequest = request.newBuilder()
                    //.addHeader(TRACE_ID_HEADER_KEY, request.header(CommonConstants.TRACE_ID_MDC_KEY))
                    //.addHeader(EVENT_HEADER_KEY, request.header(CommonConstants.EVENT_KEY))
                    .build();
            /*logger.info(String.format("Sending request %s on %s%n%s %s",
                    newRequest.url(), chain.connection(), newRequest.headers(), requestBody))*/

            logger.info(String.format("Sending request for %s",newRequest.url()),
                    new HashMap<String, Object>() {{
                        put("requestUrl",newRequest.url().url());
                        put("requestUrlParts",newRequest.url());
                        put("connection",chain.connection());
                        put("headers", newRequest.headers());
                        put("requestBody", getRequestBody(request, buffer));
                    }},
                    newRequest.url(), chain.connection(), newRequest.headers(), requestBody,
                    event
            );

            Response response = chain.proceed(newRequest);

            long t2 = System.nanoTime();
            String responseBody =response.body().string();
            /*logger.info(String.format("Received response for %s in %.1fms%n%s %s%nHttpStatus=%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers(), responseBody, response.code()));*/



            if (StringUtils.isEmpty(MDC.get(EVENT_KEY))
                    && !StringUtils.isEmpty(request.header(EVENT_HEADER_KEY))){
                MDC.put(EVENT_KEY, request.header(EVENT_HEADER_KEY));

            }

            if (StringUtils.isEmpty(MDC.get(TRACE_ID_MDC_KEY))
                    && !StringUtils.isEmpty(request.header(TRACE_ID_HEADER_KEY))){
                MDC.put(TRACE_ID_MDC_KEY, request.header(TRACE_ID_HEADER_KEY));

            }

            if (event.getEvent().equalsIgnoreCase(Events.DEFAULT_REQUEST_EVENT)){
                event = new LogEvent(Events.DEFAULT_RESPONSE_EVENT);
            }

            logger.info(String.format("Received response for %s",
                    response.request().url()),
                    new HashMap<String, Object>() {{
                    put("duration", (t2 - t1) / 1e6d);
                    put("headers", response.headers());
                    put("responseBody",responseBody);
                    put("statusCode",response.code());
                    }}, event);


            //return response;
            return response.newBuilder()
                    .body(ResponseBody.create(response.body().contentType(), responseBody))
                    .build();
        });

        OkHttpClient client = builder.build();
        return client;
    }

    @Bean
    public RetrofitRequiredHeadersEnforcerAspectJAspect requestHeadersAspect(){
        RetrofitRequiredHeadersEnforcerAspectJAspect requestHeadersAspect = Aspects.aspectOf(RetrofitRequiredHeadersEnforcerAspectJAspect.class);
        return  requestHeadersAspect;
    }

    @Bean
    public Retrofit retrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl("https://testapi")
                .addConverterFactory(ScalarsConverterFactory.create())
                //.addConverterFactory(new StringConverterFactory())
                //.addConverterFactory(JacksonConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(Java8CallAdapterFactory.create())
                .client(client)
                .build();
    }
    @Bean
    public GiphyApiService giphyApiService(OkHttpClient client, ObjectMapper objectMapper) {
        Retrofit retrofit = createRetrofit("http://testurl", client, objectMapper);
        GiphyApiService service = retrofit.create(GiphyApiService.class);
        return service;
    }

    @Bean
    public ApiGatewayService apiGatewayService(OkHttpClient client, ObjectMapper objectMapper) {
        String apiGatewayBaseUrl = env.getProperty("api.gateway.base-url");
        Retrofit retrofit = createRetrofit(apiGatewayBaseUrl, client, objectMapper);
        ApiGatewayService service = retrofit.create(ApiGatewayService.class);
        return service;
    }

    @Bean
    public AuthorizationApiService authorizationService(OkHttpClient client, ObjectMapper objectMapper) {
        String authorizationServiceBaseUrl = env.getProperty("authorization.service.endpoint");
        Retrofit retrofit = createRetrofit(authorizationServiceBaseUrl, client, objectMapper);
        AuthorizationApiService service = retrofit.create(AuthorizationApiService.class);
        return service;
    }

    @Bean
    public EmailApiService emailService(OkHttpClient client, ObjectMapper objectMapper) {
        String emailServiceBaseUrl = env.getProperty("email.service.endpoint");
        Retrofit retrofit = createRetrofit(emailServiceBaseUrl, client, objectMapper);
        EmailApiService service = retrofit.create(EmailApiService.class);
        return service;
    }

    private Retrofit createRetrofit(String baseUrl, OkHttpClient client, ObjectMapper objectMapper) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                //.addConverterFactory(ScalarsConverterFactory.create())
                //.addConverterFactory(new StringConverterFactory())
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                //.addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(Java8CallAdapterFactory.create())
                .client(client)
                .build();
    }

   // @Override
    @Bean("taskExecutor")
    public ExecutorService getAsyncExecutor() {
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        //return new LazyTraceExecutor(beanFactory, threadPoolTaskExecutor);
        return new TraceableExecutorService(executorService, this.tracer, this.traceKeys, this.spanNamer, "traceable_executor_service");
        //return taskExecutor();
    }

    /*
    @Bean("taskExecutor")
    public  ExecutorService getAsyncExecutor(){
       return MoreExecutors.newDirectExecutorService();
    }
    */
    /*@Bean
    public ExecutorService taskExecutor() {
        / *ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("async-%d").build();
        / *MdcThreadPoolExecutor mdcThreadPoolExecutor = MdcThreadPoolExecutor.newWithCurrentMdc(50,
                100,
                5000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());* /
        MdcThreadPoolExecutor mdcThreadPoolExecutor = MdcThreadPoolExecutor.newWithCurrentMdcAndThreadFactory(
                100,threadFactory);

        return mdcThreadPoolExecutor;* /
        //return CompletableExecutors.completable(mdcThreadPoolExecutor);
        // return CompletableExecutors.completable(Executors.newFixedThreadPool(10, threadFactory));

        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(50);
        threadPoolTaskExecutor.setMaxPoolSize(100);
        threadPoolTaskExecutor.initialize();

        ExecutorService executorService = Executors.newFixedThreadPool(100);

       // return new TraceableExecutorService(taskExecutor(), this.tracer, this.traceKeys, this.spanNamer, "traceable_executor_service");
       return executorService;
      //  return getAsyncExecutor();

    }*/



    // return null;
    //}
    //@Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> logger.error("Uncaught async error", ex);
    }



   /* @Bean
    public SpanLogger slf4jSpanLogger() {
        return new Log4j2SpanLogger("");
    }*/
}
