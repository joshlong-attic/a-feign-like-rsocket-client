package rsocketclient;

import com.example.rsocketfeign.client.RsocketFeignApplication;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;

// this is where we would find and register beans based on interfaces.
@Log4j2
class RSocketClientsRegistrar
		implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

	private Environment environment;
	private ResourceLoader resourceLoader;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
	                                    BeanDefinitionRegistry registry,
	                                    BeanNameGenerator importBeanNameGenerator) {
		var basePackages = this.getBasePackages(importingClassMetadata);
		var scanner = this.buildScanner();
		basePackages
				.forEach(basePackage -> scanner
						.findCandidateComponents(basePackage)
						.stream()
						.filter(cc -> cc instanceof AnnotatedBeanDefinition)
						.map(abd -> (AnnotatedBeanDefinition) abd)
						.forEach(beanDefinition -> {
							var annotationMetadata = beanDefinition.getMetadata();
							Assert.isTrue(annotationMetadata.isInterface(), "@RSocketClient must be an interface");
							var attributes = annotationMetadata.getAnnotationAttributes(RSocketClient.class.getCanonicalName());
							this.registerRSocketClient(registry, beanDefinition, annotationMetadata, attributes);
						}));
	}

	private ClassPathScanningCandidateComponentProvider buildScanner() {
		var scanner = new ClassPathScanningCandidateComponentProvider(false, this.environment) {
			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition metadata) {
				return metadata.getMetadata().isIndependent() && !metadata.getMetadata().isAnnotation();
			}
		};
		scanner.addIncludeFilter(new AnnotationTypeFilter(RSocketClient.class));
		scanner.setResourceLoader(this.resourceLoader);
		return scanner;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		// what is this method for?
	}

	private void registerRSocketClient(BeanDefinitionRegistry registry,
	                                   AnnotatedBeanDefinition abstractBeanDefinition,
	                                   AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {

		String className = annotationMetadata.getClassName();
		log.info("trying to turn the interface " + className + " into an RSocketClientFactoryBean");

		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(RSocketClientFactoryBean.class);
		definition.addPropertyValue("type", className);
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
		beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
		beanDefinition.setPrimary(true);

		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, new String[0]);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}


	private Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
		//todo replace this with a lookup in the annotation!!
		//todo hardcoded magic string alert
		return Set.of(RsocketFeignApplication.class.getPackageName());
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
}

@Log4j2
class RSocketClientFactoryBean implements ApplicationContextAware, FactoryBean<Object> {

	private Class<?> type;
	private ApplicationContext context;

	@SneakyThrows
	public void setType(String type) {
		this.type = Class.forName(type);
	}

	@Override
	public Object getObject() throws Exception {
		// todo make this more dynamic so that we can associate a particular bean to a paritcular interface.
		// right now we assume there's only per context
		RSocketRequester rSocketRequester = this.context.getBean(RSocketRequester.class);
		return RSocketClientBuilder.buildClientFor(this.type, rSocketRequester);
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}
}


@Log4j2
class RSocketClientBuilder {

	static <T> T buildClientFor(Class<T> clazz, RSocketRequester rSocketRequester) {

//		var uniqueDeclaredMethods = ReflectionUtils.getUniqueDeclaredMethods(clazz);
		var pfb = new ProxyFactoryBean();
		pfb.setTargetClass(clazz);
		pfb.setAutodetectInterfaces(true);
		pfb.addAdvice((MethodInterceptor) methodInvocation -> {
			String name = methodInvocation.getMethod().getName();
			// ok, so somebody has invoked a method on our interface. now we need to figure out what the signature on the method implies
			// we also need to inspect it for @MessageMapping annotations
			// and we need to then u
			Class<?> returnType = methodInvocation.getMethod().getReturnType();
			Object[] arguments = methodInvocation.getArguments();
			Parameter[] parameters = methodInvocation.getMethod().getParameters();
			log.info("return class: " + returnType.getName());
			log.info("return Mono?: " + Mono.class.isAssignableFrom(returnType));
			log.info("return Flux: " + Flux.class.isAssignableFrom(returnType));

			MessageMapping annotation = methodInvocation.getMethod().getAnnotation(MessageMapping.class);
			String route = annotation.value()[0];
			log.info("route: " + route);
			

			return Mono.empty();
		});
		pfb.addInterface(clazz);
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