package com.joshlong.rsocket.old.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RsocketFeignApplication {

	public static void main(String[] args) {
		System.setProperty("spring.profiles.active", "service");
		SpringApplication.run(RsocketFeignApplication.class, args);
	}

}
