package com.openstudy.openstudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class OpenstudyApplication {

	public static void main(String[] args) {
		// Keep JDBC Timestamp conversion, server-side formatting, and API date serialization
		// consistent with the application's Asia/Kolkata database/session timezone.
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Kolkata")));
		SpringApplication.run(OpenstudyApplication.class, args);
	}

}
