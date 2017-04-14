package com.quantal.exchange.users.config.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.quantal.shared.convertors.LocalDateConverter;
import com.quantal.shared.convertors.LocalDateTimeConverter;
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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.java8.Java8CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.Locale;


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
    //registry.addResourceHandler("/messages/**").addResourceLocations("/messages/");
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

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
    localeChangeInterceptor.setParamName("lang");
    registry.addInterceptor(localeChangeInterceptor);
  }

  @Bean
  public LocaleResolver localeResolver() {
    CookieLocaleResolver localeResolver = new CookieLocaleResolver();
    localeResolver.setDefaultLocale(Locale.UK); // Set default Locale as UK
    // Uncomment the lines below if you want touse the Session Locale Resolver
    // and return localeResolver
    // SessionLocaleResolver localeResolver = new SessionLocaleResolver();
    // localeResolver.setDefaultLocale(Locale.UK); // Set default Locale as UK
    return localeResolver;
  }

  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource =
            new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:messages/messages");
    messageSource.setUseCodeAsDefaultMessage(true);
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setCacheSeconds(0);
    return messageSource;
  }
}
