package com.quantal.exchange.users.config.shared;

import com.quantal.exchange.users.convertors.orika.GenderToGenderEnumOrikaBiConvertor;
import com.quantal.exchange.users.convertors.orika.UserStatusToUserStatusEnumOrikaBiConvertor;
import com.quantal.javashared.beanpostprocessors.LoggerInjectorBeanPostProcessor;
import com.quantal.javashared.dto.CommonLogFields;
import com.quantal.javashared.dto.LoggerConfig;
import com.quantal.javashared.dto.LogzioConfig;
import com.quantal.javashared.logger.QuantalLoggerFactory;
import com.quantal.javashared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.javashared.objectmapper.OrikaBeanMapper;
import com.quantal.javashared.services.implementations.MessageServiceImpl;
import com.quantal.javashared.services.interfaces.MessageService;
import ma.glasnost.orika.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Created by dman on 12/04/2017.
 */

@Configuration

@PropertySource("classpath:application.properties")
//@EnableAspectJAutoProxy
@EnableLoadTimeWeaving(aspectjWeaving= EnableLoadTimeWeaving.AspectJWeaving.ENABLED)
@EnableSpringConfigured
//@ImportResource("classpath:spring-application.xml")
public class SharedConfig {
    @Value("${spring.version}")
    private String springVersion;

    @Value("${spring.application.name}")
    private String moduleName;

    @Value("${spring.application.version}")
    private String moduleVersion;

    @Bean
    public NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper(ApplicationContext applicationContext) {

        NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper =  new NullSkippingOrikaBeanMapper(getConvertorsList());
        nullSkippingOrikaBeanMapper.setApplicationContext(applicationContext);
        return nullSkippingOrikaBeanMapper;
    }

    @Bean
    @Primary
    public OrikaBeanMapper orikaBeanMapper(ApplicationContext applicationContext) {

        OrikaBeanMapper orikaBeanMapper = new OrikaBeanMapper(getConvertorsList());
        orikaBeanMapper.setApplicationContext(applicationContext);
        return orikaBeanMapper;
    }


    @Bean
    public MessageService messageService (MessageSource messageSource){
        return new MessageServiceImpl(messageSource);
    }

    @Bean
    public LoadTimeWeaver loadTimeWeaver()  throws Throwable {
        InstrumentationLoadTimeWeaver loadTimeWeaver = new InstrumentationLoadTimeWeaver();
        return loadTimeWeaver;
    }

    /*
    @Bean
    public LoggerAspect loggerAspect() {
        LoggerAspect aspect = Aspects.aspectOf(LoggerAspect.class);
        return aspect;
    }*/

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }


    @Bean
    public CommonLogFields commonLogFields() throws UnknownHostException {
        String hostname = InetAddress.getLocalHost().getHostName();
        return new CommonLogFields(
                "java",
                "spring-boot",
                springVersion,
                moduleName,
                hostname,
                moduleVersion,
                Locale.UK.toString(),
                Instant.now().toString()
                );
    }

    @Bean
    public LogzioConfig logzioConfig(@Value("${logzio.token}") String logzioToken) {
        return QuantalLoggerFactory.createDefaultLogzioConfig(logzioToken, Optional.empty(), Optional.empty());
    }


    /*@Bean
    public QuantalLogger quantalLogger( CommonLogFields commonLogFields, LogzioConfig logzioConfig){
        LoggerConfig loggerConfig = new LoggerConfig();
        loggerConfig.setLogzioConfig(logzioConfig);
        loggerConfig.setCommonLogFields(commonLogFields);
        return  QuantalLoggerFactory.getLogzioLogger(this.getClass(), loggerConfig);
    }*/

    @Bean
    public LoggerConfig loggerConfig(LogzioConfig logzioConfig, CommonLogFields commonLogFields){
        LoggerConfig loggerConfig = LoggerConfig.builder().build();
        loggerConfig.setLogzioConfig(logzioConfig);
        loggerConfig.setCommonLogFields(commonLogFields);
        return loggerConfig;
    }
    @Bean
    public LoggerInjectorBeanPostProcessor loggerInjectorBeanPostProcessor(CommonLogFields commonLogFields, LogzioConfig logzioConfig){
        return new LoggerInjectorBeanPostProcessor(commonLogFields, logzioConfig);
    }

    /*
    @Bean
    public RequestHeadersAspect retrofitRequiredHeadersEnforcerAspect(){
        Set<String> headers = new HashSet<>();
        headers.add("X-Event");
        headers.add("X-TraceId");
        RequestHeadersAspect requestHeadersAspect = new RequestHeadersAspect(headers);
        return  requestHeadersAspect;
    }*/

    private List<Converter> getConvertorsList(){
        List<Converter> convertorsList = new ArrayList<>();
        convertorsList.add(new GenderToGenderEnumOrikaBiConvertor());
        convertorsList.add(new UserStatusToUserStatusEnumOrikaBiConvertor());
        return convertorsList;
    }

}
