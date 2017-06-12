package com.quantal.exchange.users.config.shared;

import com.quantal.shared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.shared.objectmapper.OrikaBeanMapper;
import com.quantal.shared.services.implementations.MessageServiceImpl;
import com.quantal.shared.services.interfaces.MessageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Created by dman on 12/04/2017.
 */

@Configuration
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

}
