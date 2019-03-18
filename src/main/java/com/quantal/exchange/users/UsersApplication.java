package com.quantal.exchange.users;

import de.invesdwin.instrument.DynamicInstrumentationLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
//@ImportAutoConfiguration(TracerAutoConfiguration.class)
public class UsersApplication {

	static {
		//Starts the aspectj weaver so that we can weave the compile time aspects
		DynamicInstrumentationLoader.waitForInitialized(); //dynamically attach java agent to jvm if not already present
		DynamicInstrumentationLoader.initLoadTimeWeavingContext(); //weave all classes before they are loaded as beans
	}

	public static void main(String[] args) {
		SpringApplication.run(UsersApplication.class, args);
	}
}
