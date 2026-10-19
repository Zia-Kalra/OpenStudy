package com.openstudy.openstudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpenstudyApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenstudyApplication.class, args);
	}

}
