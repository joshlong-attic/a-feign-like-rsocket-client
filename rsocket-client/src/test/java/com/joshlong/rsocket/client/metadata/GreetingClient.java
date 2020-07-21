package com.joshlong.rsocket.client.metadata;

import com.joshlong.rsocket.client.RSocketClient;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import reactor.core.publisher.Mono;

@RSocketClient
interface GreetingClient {

	@MessageMapping("greetings")
	Mono<String> greet(@Header(Constants.CLIENT_ID_MIME_TYPE_VALUE) String clientId, @Payload Mono<String> name);

}
