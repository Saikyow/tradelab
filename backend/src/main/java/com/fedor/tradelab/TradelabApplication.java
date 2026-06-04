package com.fedor.tradelab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TradelabApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradelabApplication.class, args);
	}

}
