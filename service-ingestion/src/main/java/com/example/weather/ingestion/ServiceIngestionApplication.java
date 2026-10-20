package com.example.weather.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.weather")
public class ServiceIngestionApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceIngestionApplication.class, args);
	}

}
