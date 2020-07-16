package com.example.rsocketfeign.service;

import com.example.rsocketfeign.GreetingResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Stream;

@SpringBootApplication
public class RsocketFeignApplication {

	public static void main(String[] args) {
		System.setProperty("spring.profiles.active", "service");
		SpringApplication.run(RsocketFeignApplication.class, args);
	}

}

@Controller
class GreetingsController {

	@MessageMapping("greetings-with-channel")
	Flux<GreetingResponse> greetParams(Flux<String> names) {
		return names
				.map(String::toUpperCase)
				.map(GreetingResponse::new);
	}

	private Flux<GreetingResponse> greet(String name) {
		return Flux
				.fromStream(Stream.generate(() -> new GreetingResponse("Hello " + name + "!")))
				.delayElements(Duration.ofSeconds(1));
	}

	@MessageMapping("greetings-stream")
	Flux<GreetingResponse> greetFlux(Mono<String> name) {
		return name.flatMapMany(this::greet);
	}

	@MessageMapping("greetings-with-name")
	Mono<GreetingResponse> greetMono(Mono<String> name) {
		return name.map(GreetingResponse::new);
	}

	@MessageMapping("greetings")
	Mono<GreetingResponse> greet() {
		return Mono.just(new GreetingResponse("Hello, world!"));
	}
}