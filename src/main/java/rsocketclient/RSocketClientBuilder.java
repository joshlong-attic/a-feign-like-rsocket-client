package rsocketclient;

import com.example.rsocketfeign.client.GreetingClient;
import lombok.extern.log4j.Log4j2;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.reactive.DestinationVariableMethodArgumentResolver;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.RouteMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
class RSocketClientBuilder {

    static <T> T buildClientFor(
            Class<T> clazz,
//            ConversionService conversionService,
            RSocketRequester rSocketRequester) {

        var pfb = new ProxyFactoryBean();
        pfb.setTargetClass(clazz);
        pfb.addInterface(clazz);
        pfb.setAutodetectInterfaces(true);
        pfb.addAdvice((MethodInterceptor) methodInvocation -> {
            String methodName = methodInvocation.getMethod().getName();
            Class<?> returnType = methodInvocation.getMethod().getReturnType();
            Object[] arguments = methodInvocation.getArguments();
            Parameter[] parameters = methodInvocation.getMethod().getParameters();
            MessageMapping annotation = methodInvocation.getMethod().getAnnotation(MessageMapping.class);
            String route = annotation.value()[0];
            ResolvableType resolvableType = ResolvableType.forMethodReturnType(methodInvocation.getMethod());
            Class<?> rawClassForReturnType = resolvableType.getGenerics()[0].getRawClass(); // this is T for Mono<T> or Flux<T>
            Object[] routeArguments = findDestinationVariables(arguments, parameters);
            Object payloadArgument = findPayloadArgument(arguments, parameters);

            if (log.isDebugEnabled()) {
                log.debug("invoking " + methodName + " accepting " + arguments.length + " argument(s) for route " + route +
                        " with destination variables (" + StringUtils.arrayToDelimitedString(routeArguments, ", ") + ")" +
                        '.' + " The payload is " + payloadArgument);
            }

            if (Mono.class.isAssignableFrom(returnType)) {
                // special case for fire-and-forget
                if (Void.class.isAssignableFrom(rawClassForReturnType)) {
                    if (log.isDebugEnabled()) {
                        log.debug("fire-and-forget");
                    }
                    return rSocketRequester
                            .route(route, routeArguments)
                            .data(payloadArgument)
                            .send();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("request-response");
                    }
                    return rSocketRequester
                            .route(route, routeArguments)
                            .data(payloadArgument)
                            .retrieveMono(rawClassForReturnType);
                }
            }

            if (Flux.class.isAssignableFrom(returnType)) {
                if (log.isDebugEnabled()) {
                    log.debug("request-stream or channel");
                }
                return rSocketRequester
                        .route(route, routeArguments)
                        .data(payloadArgument)
                        .retrieveFlux(rawClassForReturnType);
            }
            // if the return type is void ...
            return Mono.empty();
        });

        return (T) pfb.getObject();
    }

    private static Object[] findDestinationVariables(Object[] arguments, Parameter[] parameters) {
        List<Object> destinationVariableValues = new ArrayList<>();
        for (var i = 0; i < arguments.length; i++) {
            var parameter = parameters[i];
            var arg = arguments[i];
            if (parameter.getAnnotationsByType(DestinationVariable.class).length > 0) {
                destinationVariableValues.add(arg);
            }
        }
        return destinationVariableValues.toArray(new Object[0]);
    }


    private static Object findPayloadArgument(Object[] arguments, Parameter[] parameters) {
        Object payloadArgument = null;
        if (arguments.length == 0) {
            payloadArgument = Mono.empty();
        } else if (arguments.length == 1) {
            payloadArgument = arguments[0];
        } else {
            Assert.isTrue(parameters.length == arguments.length, "there should be " +
                    "an equal number of " + Parameter.class.getName() + " and objects");
            for (var i = 0; i < parameters.length; i++) {
                Parameter annotations = parameters[i];
                Object argument = arguments[i];
                if (annotations.getAnnotationsByType(Payload.class).length > 0) {
                    payloadArgument = argument;
                }
            }
        }
        Assert.notNull(payloadArgument, "you must specify a @Payload parameter OR just one parameter");
        return payloadArgument;
    }

    // mono -> flux
    private Flux<?> fluxInMonoOut(Mono<?> in) {
        return null;
    }

    // mono -> mono
    private Mono<?> monoInAndOut(Mono<?> in) {
        return null;
    }

    // flux -> mono
    private Mono<?> fluxInMonoOut(Flux<?> in) {
        return null;
    }

    // flux -> flux
    private Flux<?> fluxInFluxOut(Flux<?> in) {
        return null;
    }


    public static void test() {
        String route = "hello.{name}.{age}";
        PathPatternRouteMatcher pathPatternRouteMatcher = new PathPatternRouteMatcher();
        RouteMatcher.Route route1 = pathPatternRouteMatcher.parseRoute(route);
        Map<String, String> vars = pathPatternRouteMatcher.matchAndExtract(route, route1);
        vars.forEach((k, v) -> log.info(k + '=' + v));
        ConversionService conversionService = ApplicationConversionService.getSharedInstance();
        DestinationVariableMethodArgumentResolver argumentResolver = new DestinationVariableMethodArgumentResolver(conversionService);
        Class<GreetingClient> clientClass = GreetingClient.class;
        Method uniqueDeclaredMethods = ReflectionUtils.getUniqueDeclaredMethods(clientClass,
                method -> method.getName().equalsIgnoreCase("greetMonoNameDestinationVariable"))[0];
        Parameter[] parameters = uniqueDeclaredMethods.getParameters();
        for (Parameter p : parameters) {
            MethodParameter methodParameter = MethodParameter.forParameter(p);
            if (argumentResolver.supportsParameter(methodParameter)) {

                Message<String> header = MessageBuilder
                        .withPayload("")
                        .setHeader(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars)
                        .build();
                Mono<Object> objectMono = argumentResolver.resolveArgument(methodParameter, header);
                objectMono.subscribe(System.out::println);
            }
        }

        //        MethodParameter parameter = MethodParameter.forParameter();
//        argumentResolver.resolveArgumentValue()

//

    /*    Stream
                .of(parameters)
                .map(MethodParameter::forParameter)
                .filter(argumentResolver::supportsParameter)
                .map(mp -> {
                    var message = MessageBuilder
                            .withPayload("")
                            .setHeader(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, annotation.value()[0])
                            .build();
                    log.info("resolving parameters for mapping string " + route + '.');
                    var argumentValue = argumentResolver.resolveArgumentValue(mp, message);
                    log.info("te result is " + argumentValue);
                    return argumentValue;
                })
                .forEach(log::info);*/

    }

}
