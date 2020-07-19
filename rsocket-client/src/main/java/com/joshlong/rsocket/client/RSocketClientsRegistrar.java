package com.joshlong.rsocket.client;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Log4j2
class RSocketClientsRegistrar implements BeanFactoryPostProcessor, ImportBeanDefinitionRegistrar, BeanFactoryAware,
		EnvironmentAware, ResourceLoaderAware {

	private BeanFactory beanFactory;

	private Environment environment;

	private ResourceLoader resourceLoader;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {
		Assert.isTrue(importingClassMetadata instanceof StandardAnnotationMetadata,
				"we need a valid reference to " + StandardAnnotationMetadata.class.getName());
		StandardAnnotationMetadata standardAnnotationMetadata = (StandardAnnotationMetadata) importingClassMetadata;
		Collection<String> basePackages = AutoConfigurationPackages.has(this.beanFactory)
				? AutoConfigurationPackages.get(this.beanFactory)
				: Arrays.asList(standardAnnotationMetadata.getIntrospectedClass().getPackage().getName());
		ClassPathScanningCandidateComponentProvider scanner = this.buildScanner();
		basePackages.forEach(basePackage -> scanner.findCandidateComponents(basePackage)//
				.stream()//
				.filter(cc -> cc instanceof AnnotatedBeanDefinition)//
				.map(abd -> (AnnotatedBeanDefinition) abd)//
				.forEach(beanDefinition -> {
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					this.validateInterface(annotationMetadata);
					this.registerRSocketClient(annotationMetadata, registry);
				}));
	}

	@SneakyThrows
	private void validateInterface(AnnotationMetadata annotationMetadata) {
		Assert.isTrue(annotationMetadata.isInterface(),
				"the @" + RSocketClient.class.getName() + " annotation must be used only on an interface");
		Class<?> clzz = Class.forName(annotationMetadata.getClassName());
		ReflectionUtils.doWithMethods(clzz, method -> {
			if (log.isDebugEnabled()) {
				log.debug("validating " + clzz.getName() + "#" + method.getName());
			}
			MessageMapping annotation = method.getAnnotation(MessageMapping.class);
			Assert.notNull(annotation, "you must use the @" + MessageMapping.class.getName()
					+ " annotation on every method on " + clzz.getName() + '.');
		});
	}

	private ClassPathScanningCandidateComponentProvider buildScanner() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false,
				this.environment) {
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
	}

	@SneakyThrows
	private void registerRSocketClient(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

		String className = annotationMetadata.getClassName();
		if (log.isDebugEnabled()) {
			log.debug("trying to turn the interface " + className + " into an RSocketClientFactoryBean");
		}

		////////////////////////////////////
		// Assert.notNull(this.beanFactory, "the beanFactory is not null");
		//
		// String typeName = Qualifier.class.getTypeName();
		// log.info(typeName);
		//
		// MergedAnnotations annotations = annotationMetadata.getAnnotations();
		// boolean hasAQualifier =
		// annotationMetadata.hasAnnotation(Qualifier.class.getTypeName());
		// log.info(hasAQualifier);
		//
		// annotations.forEach(ma -> log.info(ma.toString()));
		// //todo how do we find the RSocketRequester in the context? How do we find it by
		// @Qualifier?

		// String rSocketRequesterBeanName = "";

		////////////////////////////////////

		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(RSocketClientFactoryBean.class);
		definition.addPropertyValue("type", className);

		// if (StringUtils.hasText(rSocketRequesterBeanName)) {
		// definition.addPropertyReference("requester", rSocketRequesterBeanName);
		// }

		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

		this.qualifyRSocketRequester(annotationMetadata, beanDefinition);

		beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
		beanDefinition.setPrimary(true);

		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, new String[0]);

		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

	/**
	 * so the important thing to figure out here is if the interface itself has been
	 * annotated with a @Qualifier. If so, then we need to find the RSocketRequster that's
	 * been annotated with that as well and use that one. Otherwise, use the single one
	 * created int the app context. If none have been defined, throw an exception.
	 */
	private void qualifyRSocketRequester(AnnotationMetadata annotationMetadata, AbstractBeanDefinition beanDefinition) {
		boolean hasAQualifier = annotationMetadata.hasMetaAnnotation(Qualifier.class.getTypeName())
				|| annotationMetadata.hasAnnotation(Qualifier.class.getTypeName());
		if (log.isDebugEnabled()) {
			log.debug("qualified? " + hasAQualifier);
		}
		AutowireCandidateQualifier autowireCandidateQualifier = new AutowireCandidateQualifier("");
		beanDefinition.addQualifier(autowireCandidateQualifier);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		log.info("postProcessBeanFactor");

	}

}
