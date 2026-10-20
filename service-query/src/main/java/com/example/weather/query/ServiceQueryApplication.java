package com.example.weather.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.weather")
public class ServiceQueryApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceQueryApplication.class, args);
	}

}
