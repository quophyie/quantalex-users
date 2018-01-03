package com.quantal.exchange.users.config.shared;

import com.quantal.exchange.users.aspects.LoggerAspect;
import com.quantal.javashared.beanpostprocessors.LoggerInjectorBeanPostProcessor;
import com.quantal.javashared.dto.CommonLogFields;
import com.quantal.javashared.dto.LogzioConfig;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.logger.QuantalLoggerFactory;
import com.quantal.javashared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.javashared.objectmapper.OrikaBeanMapper;
import com.quantal.javashared.services.implementations.MessageServiceImpl;
import com.quantal.javashared.services.interfaces.MessageService;
import org.aspectj.lang.Aspects;
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
import java.util.Locale;
import java.util.Optional;

/**
 * Created by dman on 12/04/2017.
 */

@Configuration

@PropertySource("classpath:application.properties")
//@EnableAspectJAutoProxy
//@EnableLoadTimeWeaving(aspectjWeaving= EnableLoadTimeWeaving.AspectJWeaving.ENABLED)
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

        NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper =  new NullSkippingOrikaBeanMapper();
        nullSkippingOrikaBeanMapper.setApplicationContext(applicationContext);
        return nullSkippingOrikaBeanMapper;
    }

    @Bean
    @Primary
    public OrikaBeanMapper orikaBeanMapper() {
        return new OrikaBeanMapper();
    }


    @Bean
    public MessageService messageService (MessageSource messageSource){
        return new MessageServiceImpl(messageSource);
    }

   /* @Bean
    public LoadTimeWeaver loadTimeWeaver()  throws Throwable {
        InstrumentationLoadTimeWeaver loadTimeWeaver = new InstrumentationLoadTimeWeaver();
        return loadTimeWeaver;
    }

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

    @Bean
    public QuantalLogger quantalLogger( CommonLogFields commonLogFields, LogzioConfig logzioConfig){
        return  QuantalLoggerFactory.getLogzioLogger(this.getClass(), commonLogFields ,logzioConfig);
    }

    @Bean
    public LoggerInjectorBeanPostProcessor loggerInjectorBeanPostProcessor(CommonLogFields commonLogFields, LogzioConfig logzioConfig){
        return new LoggerInjectorBeanPostProcessor(commonLogFields, logzioConfig);
    }

}
