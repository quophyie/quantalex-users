package com.quantal.exchange.users.config.shared;

import com.quantal.exchange.users.aspects.LoggerAspect;
import com.quantal.shared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.shared.objectmapper.OrikaBeanMapper;
import com.quantal.shared.services.implementations.MessageServiceImpl;
import com.quantal.shared.services.interfaces.MessageService;
import org.aspectj.lang.Aspects;
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

    @Bean
    public LoadTimeWeaver loadTimeWeaver()  throws Throwable {
        InstrumentationLoadTimeWeaver loadTimeWeaver = new InstrumentationLoadTimeWeaver();
        return loadTimeWeaver;
    }

    @Bean
    public LoggerAspect loggerAspect() {
        LoggerAspect aspect = Aspects.aspectOf(LoggerAspect.class);
        return aspect;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }


}
