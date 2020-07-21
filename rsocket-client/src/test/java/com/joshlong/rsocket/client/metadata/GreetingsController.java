package com.joshlong.rsocket.client.metadata;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;

@Log4j2
@Profile("service")
@Controller
class GreetingsController {

	@MessageMapping("greetings")
	Mono<String> greet(@Headers Map<String, Object> headers, @Payload Mono<String> in) {
		headers.forEach((k, v) -> log.info(k + '=' + v));
		return in.map(String::toUpperCase);
	}

}
