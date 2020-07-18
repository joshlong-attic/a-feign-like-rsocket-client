package com.joshlong.rsocket.client.client;

import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class RsocketFeignApplication {

	@SneakyThrows
	public static void main(String[] args) {
		SpringApplication.run(RsocketFeignApplication.class, args);
		System.in.read();
	}

	@Bean
	RSocketRequester rSocketRequester(RSocketRequester.Builder builder) {
		return builder.connectTcp("localhost", 8888).block();
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> client(GreetingClient greetingClient) {
		return are -> {
			greetingClient.greetFireAndForget(Mono.just("Spring Fans")).subscribe();
			greetingClient.greetParams(Flux.just("one", "two")).subscribe(System.out::println);
			greetingClient.greet().subscribe(System.out::println);
			greetingClient.greet(Mono.just("Spring Fans")).subscribe(System.out::println);
			greetingClient.greetStream(Mono.just("Spring fans over and over")).subscribe(System.out::println);
			greetingClient.greetMonoNameDestinationVariable("jlong", 36, Mono.just("Josh"))
					.subscribe(System.out::println);
		};
	}

}
