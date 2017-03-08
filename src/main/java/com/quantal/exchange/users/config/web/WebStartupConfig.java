package com.quantal.exchange.users.config.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.quantal.exchange.users.convertors.LocalDateConverter;
import com.quantal.exchange.users.convertors.LocalDateTimeConverter;
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

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

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
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("/messages");
    messageSource.setDefaultEncoding("UTF-8");
    return messageSource;
  }
}
