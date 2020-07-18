package rsocketclient;

import lombok.extern.log4j.Log4j2;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Parameter;

@Log4j2
class RSocketClientBuilder {

    static <T> T buildClientFor(Class<T> clazz, RSocketRequester rSocketRequester) {

//		var uniqueDeclaredMethods = ReflectionUtils.getUniqueDeclaredMethods(clazz);
        var pfb = new ProxyFactoryBean();
        pfb.setTargetClass(clazz);
        pfb.addInterface(clazz);
        pfb.setAutodetectInterfaces(true);
        pfb.addAdvice((MethodInterceptor) methodInvocation -> {
            String methodName = methodInvocation.getMethod().getName();
            // ok, so somebody has invoked a method on our interface. now we need to figure out what the signature on the method implies
            // we also need to inspect it for @MessageMapping annotations
            // and we need to then u
            Class<?> returnType = methodInvocation.getMethod().getReturnType();
            Object[] arguments = methodInvocation.getArguments();
            Parameter[] parameters = methodInvocation.getMethod().getParameters();
            log.debug("invoking method " + methodName + '.');
            log.debug("return class: " + returnType.getName());
            log.debug("return Mono: " + Mono.class.isAssignableFrom(returnType));
            log.debug("return Flux: " + Flux.class.isAssignableFrom(returnType));

            MessageMapping annotation = methodInvocation.getMethod().getAnnotation(MessageMapping.class);
            String route = annotation.value()[0];
            log.info("route: " + route);


            ResolvableType resolvableType = ResolvableType.forMethodReturnType(methodInvocation.getMethod());
            Class<?> rawClassForReturnType = resolvableType.getGenerics()[0].getRawClass(); // this is T for Mono<T> or Flux<T>

            Object argument = arguments.length == 0 ? Mono.empty() : methodInvocation.getArguments()[0];

            if (Mono.class.isAssignableFrom(returnType)) {
                // special case for fire-and-forget
                if (Void.class.isAssignableFrom( rawClassForReturnType )) {
                	log.debug ("fire-and-forget");
                    return rSocketRequester
                            .route(route)
                            .data(argument)
                            .send();
                }
                else {
                	log.info ("returning Mono<T> and accepting  "+ arguments.length + " arguments");
                    return rSocketRequester
                            .route(route)
                            .data(argument)
                            .retrieveMono(rawClassForReturnType);
                }
            }

            if (Flux.class.isAssignableFrom(returnType)) {
                return rSocketRequester
                        .route(route)
                        .data(argument)
                        .retrieveFlux(rawClassForReturnType);
            }


            // if the return type is void ...
            return Mono.empty();
        });

        return (T) pfb.getObject();
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


}
