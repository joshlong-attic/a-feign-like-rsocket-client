package com.joshlong.rsocket.client;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.SocketUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

public class RSocketClientTest {

	static ConfigurableApplicationContext applicationContext;

	static AtomicInteger port = new AtomicInteger(SocketUtils.findAvailableTcpPort());

	@BeforeAll
	public static void begin() throws Exception {
		applicationContext = new SpringApplicationBuilder(RSocketServerConfiguration.class).web(WebApplicationType.NONE)
				.run("--spring.profiles.active=service", "--spring.rsocket.server.port=" + port.get());
	}

	@AfterAll
	public static void destroy() {
		applicationContext.stop();
	}

	@Test
	public void noValueInMonoOut() throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(RSocketClientConfiguration.class)
				.web(WebApplicationType.NONE).run("--service.port=" + port.get(), "--spring.profiles.active=client");
		GreetingClient greetingClient = context.getBean(GreetingClient.class);
		Mono<GreetingResponse> greet = greetingClient.greet();
		StepVerifier.create(greet).expectNextMatches(gr -> gr.getMessage().equalsIgnoreCase("Hello, world!"))
				.verifyComplete();

	}

}

@Profile("client")
@SpringBootApplication
@EnableRSocketClients
class RSocketClientConfiguration {

	@Bean
	RSocketRequester rSocketRequester(@Value("${service.port}") int port, RSocketRequester.Builder builder) {
		return builder.connectTcp("localhost", port).block();
	}

}

@Log4j2
@Profile("service")
@Configuration
@EnableAutoConfiguration
class RSocketServerConfiguration {

	@Bean
	GreetingsController greetingsController() {
		return new GreetingsController();
	}

	@PostConstruct
	public void start() {
		log.info("starting " + RSocketServerConfiguration.class.getName() + '.');
	}

}