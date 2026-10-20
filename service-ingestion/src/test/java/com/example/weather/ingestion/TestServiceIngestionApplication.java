package com.example.weather.ingestion;

import org.springframework.boot.SpringApplication;

public class TestServiceIngestionApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServiceIngestionApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
