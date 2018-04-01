package com.quantal.exchange.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.scheduling.annotation.EnableScheduling;
//import org.zalando.tracer.spring.TracerAutoConfiguration;

@SpringBootApplication
@EnableScheduling
//@ImportAutoConfiguration(TracerAutoConfiguration.class)
public class UsersApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsersApplication.class, args);
	}
}
