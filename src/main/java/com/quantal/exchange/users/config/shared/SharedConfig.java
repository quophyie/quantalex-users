package com.quantal.exchange.users.config.shared;

import com.quantal.basecomponents.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.basecomponents.objectmapper.OrikaBeanMapper;
import com.quantal.basecomponents.services.implementations.MessageServiceImpl;
import com.quantal.basecomponents.services.interfaces.MessageService;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by dman on 12/04/2017.
 */

@Configuration
public class SharedConfig {

    @Bean
    public NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper() {
        return new NullSkippingOrikaBeanMapper();
    }

    @Bean
    public OrikaBeanMapper orikaBeanMapper() {
        return new OrikaBeanMapper();
    }


    @Bean
    public MessageService messageService (MessageSource messageSource){
        return new MessageServiceImpl(messageSource);
    }

}
