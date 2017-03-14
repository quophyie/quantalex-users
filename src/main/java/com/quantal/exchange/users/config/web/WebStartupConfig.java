package com.quantal.exchange.users.config.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.quantal.exchange.users.convertors.LocalDateConverter;
import com.quantal.exchange.users.convertors.LocalDateTimeConverter;
import com.quantal.exchange.users.services.api.GiphyApiService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.java8.Java8CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


/**
 * Created by dman on 08/03/2017.
 */

@Configuration
//@EnableWebMvc
public class WebStartupConfig extends WebMvcConfigurerAdapter {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String myExternalFilePath = "classpath:/static/";

    registry.addResourceHandler("**/*").addResourceLocations(myExternalFilePath);

    super.addResourceHandlers(registry);
  }

  /*@Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    //log.info("Configuring http message converters...");
    MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
    List<MediaType> types = new ArrayList<>(1);
    types.add(MediaType.APPLICATION_JSON);

    jacksonConverter.setSupportedMediaTypes(types);
    //jacksonConverter.setObjectMapper(objectMapper);
    converters.add(jacksonConverter);
  } */

  @Bean
  @Primary
  public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    ObjectMapper objectMapper = builder.createXmlMapper(false).build();
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//        objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    return objectMapper;
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new LocalDateConverter("yyyy-MM-dd"));
    registry.addConverter(new LocalDateTimeConverter("yyyy-MM-dd'T'HH:mm:ss.SSS"));
  }

  @Bean
  public OkHttpClient okHttpClient() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();;
    Logger logger = LoggerFactory.getLogger("LoggingInterceptor");

    builder.interceptors().add(chain -> {
      Request request = chain.request();

      long t1 = System.nanoTime();
      logger.info(String.format("Sending request %s on %s%n%s",
              request.url(), chain.connection(), request.headers()));

      Response response = chain.proceed(request);

      long t2 = System.nanoTime();
      logger.info(String.format("Received response for %s in %.1fms%n%s",
              response.request().url(), (t2 - t1) / 1e6d, response.headers()));

      return response;
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
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("/messages");
    messageSource.setDefaultEncoding("UTF-8");
    return messageSource;
  }
}
