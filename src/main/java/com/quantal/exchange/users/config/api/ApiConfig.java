package com.quantal.exchange.users.config.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantal.exchange.users.services.api.ApiGatewayService;
import com.quantal.exchange.users.services.api.AuthorizationService;
import com.quantal.exchange.users.services.api.EmailService;
import com.quantal.exchange.users.services.api.GiphyApiService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import retrofit2.Retrofit;
import retrofit2.adapter.java8.Java8CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Created by dman on 14/03/2017.
 *
 * This class should contain the Retrofit API interfaces
 */

@Configuration
public class ApiConfig
{

    private Environment env;
    @Autowired
    public ApiConfig(Environment env){
        this.env = env;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();;
        Logger logger = LoggerFactory.getLogger("LoggingInterceptor");

        builder.interceptors().add(chain -> {
            String requestBody = "";
            Request request = chain.request();
            final Buffer buffer = new Buffer();
            if (request.body() != null) {
                request.body().writeTo(buffer);
                requestBody = buffer.readUtf8();
            }
            long t1 = System.nanoTime();
            logger.info(String.format("Sending request %s on %s%n%s %s",
                    request.url(), chain.connection(), request.headers(), requestBody));

            Response response = chain.proceed(request);

            long t2 = System.nanoTime();
            String responseBody =response.body().string();
            logger.info(String.format("Received response for %s in %.1fms%n%s %s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers(), responseBody));

            //return response;
            return response.newBuilder()
                    .body(ResponseBody.create(response.body().contentType(), responseBody))
                    .build();
        });

        OkHttpClient client = builder.build();
        return client;
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
    public AuthorizationService authorizationService(OkHttpClient client, ObjectMapper objectMapper) {
        String authorizationServiceBaseUrl = env.getProperty("authorization.service.endpoint");
        Retrofit retrofit = createRetrofit(authorizationServiceBaseUrl, client, objectMapper);
        AuthorizationService service = retrofit.create(AuthorizationService.class);
        return service;
    }

    @Bean
    public EmailService emailService(OkHttpClient client, ObjectMapper objectMapper) {
        String emailServiceBaseUrl = env.getProperty("email.service.endpoint");
        Retrofit retrofit = createRetrofit(emailServiceBaseUrl, client, objectMapper);
        EmailService service = retrofit.create(EmailService.class);
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
}
