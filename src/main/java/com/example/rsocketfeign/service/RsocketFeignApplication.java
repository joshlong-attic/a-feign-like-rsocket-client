package com.example.rsocketfeign.service;

import com.example.rsocketfeign.GreetingResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class RsocketFeignApplication {

	public static void main(String[] args) {
		System.setProperty("spring.profiles.active", "service");
		SpringApplication.run(RsocketFeignApplication.class, args);
	}

}

@Controller
class GreetingsController {

	@MessageMapping("greetings")
	Mono<GreetingResponse> greet() {
		return Mono.just(new GreetingResponse("Hello, world!"));
	}
}