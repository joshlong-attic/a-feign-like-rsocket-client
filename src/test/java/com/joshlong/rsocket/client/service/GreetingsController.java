package com.joshlong.rsocket.client.service;

import com.joshlong.rsocket.client.GreetingResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Stream;

@Log4j2
@Controller
class GreetingsController {

	@MessageMapping("greetings-mono-name.{name}.{age}")
	Mono<String> greetMonoNameDestinationVariable(@DestinationVariable("name") String name,
			@DestinationVariable("age") int age, @Payload Mono<String> payload) {
		log.info("name=" + name);
		log.info("age=" + age);
		return payload;
	}

	@MessageMapping("fire-and-forget")
	Mono<Void> fireAndForget(Mono<String> valueIn) {
		return valueIn.doOnNext(value -> log.info("received fire-and-forget " + value + '.'))
				.flatMap(x -> Mono.empty());
	}

	@MessageMapping("greetings-with-channel")
	Flux<GreetingResponse> greetParams(Flux<String> names) {
		return names.map(String::toUpperCase).map(GreetingResponse::new);
	}

	@MessageMapping("greetings-stream")
	Flux<GreetingResponse> greetFlux(Mono<String> name) {
		return name.flatMapMany(
				name1 -> Flux.fromStream(Stream.generate(() -> new GreetingResponse("Hello " + name1 + "!")))
						.delayElements(Duration.ofSeconds(1)))
				.take(5);
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
