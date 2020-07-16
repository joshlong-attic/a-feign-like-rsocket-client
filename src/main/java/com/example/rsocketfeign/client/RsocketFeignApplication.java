package com.example.rsocketfeign.client;

import com.example.rsocketfeign.GreetingResponse;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rsocketclient.RSocketClient;

@SpringBootApplication
public class RsocketFeignApplication {

	@SneakyThrows
	public static void main(String[] args) {
		SpringApplication.run(RsocketFeignApplication.class, args);
		System.in.read();
	}

	@Bean
	RSocketRequester rSocketRequester(RSocketRequester.Builder builder) {
		return builder
				.connectTcp("localhost", 8888)
				.block();
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> client(GreetingClient greetingClient) {
		return are -> {
			Mono <GreetingResponse> greet = greetingClient.greet();
		};
	}

}

@RSocketClient
interface GreetingClient {

	@MessageMapping("greetings")
	Mono<GreetingResponse> greet();
}

