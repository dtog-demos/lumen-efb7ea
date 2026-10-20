package com.example.weather.query;

import org.springframework.boot.SpringApplication;

public class TestServiceQueryApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServiceQueryApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
