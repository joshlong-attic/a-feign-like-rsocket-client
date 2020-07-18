package com.joshlong.rsocket.client;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RSocketClientsRegistrar.class)
public @interface EnableRSocketClients {

}