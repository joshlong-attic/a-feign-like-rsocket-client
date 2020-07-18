# A Feign-like RSocket Client 

## Installation

Add the following dependency to your build: 

```xml
<dependency>
    <groupId>com.joshlong.rsocket</groupId>
    <artifactId>client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

In your Java code you need to enable the RSocket client support. Use the `@EnableRSocketClient` annotation. You'll also need to define an `RSocketRequester` bean. 

```java
@SpringBootApplication
@EnableRSocketClient
class RSocketClientApplication {
 
 @Bean RSocketRequester requester (RSocketRequester.Builder builder) {
  return builder.connectTcp("localhost", 8888).block();
 }
}

``` 

then, define an RSocket client interface, like this:


```java 


@RSocketClient
public interface GreetingClient {

	@MessageMapping("supplier")
	Mono<GreetingResponse> greet();

	@MessageMapping("request-response")
	Mono<GreetingResponse> requestResponse(Mono<String> name);

	@MessageMapping("fire-and-forget")
	Mono<Void> fireAndForget(Mono<String> name);

	@MessageMapping("destination.variables.and.payload.annotations.{name}.{age}")
	Mono<String> greetMonoNameDestinationVariable(
            @DestinationVariable("name") String name,
			@DestinationVariable("age") int age, 
            @Payload Mono<String> payload);
}

```

If you invoke methods on this interface it'll in turn invoke endpoints using the configured `RSocketRequester` for you, turning destination variables into route variables and turning your payload into the data for the request.