package com.joshlong.rsocket.client.service;

import com.joshlong.rsocket.client.GreetingResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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

