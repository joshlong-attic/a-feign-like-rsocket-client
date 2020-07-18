package com.joshlong.rsocket.client;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Log4j2
class RSocketClientsRegistrar
		implements ImportBeanDefinitionRegistrar, BeanFactoryAware, EnvironmentAware, ResourceLoaderAware {

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
		basePackages.forEach(basePackage -> scanner.findCandidateComponents(basePackage).stream()
				.filter(cc -> cc instanceof AnnotatedBeanDefinition).map(abd -> (AnnotatedBeanDefinition) abd)
				.forEach(beanDefinition -> {
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					Assert.isTrue(annotationMetadata.isInterface(),
							"the @" + RSocketClient.class.getName() + " annotation must be used only on an interface");
					Map<String, Object> attributes = annotationMetadata
							.getAnnotationAttributes(RSocketClient.class.getCanonicalName());
					this.registerRSocketClient(registry, beanDefinition, annotationMetadata, attributes);
				}));
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

	private void registerRSocketClient(BeanDefinitionRegistry registry, AnnotatedBeanDefinition abstractBeanDefinition,
			AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {

		String className = annotationMetadata.getClassName();
		if (log.isDebugEnabled()) {
			log.debug("trying to turn the interface " + className + " into an RSocketClientFactoryBean");
		}

		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(RSocketClientFactoryBean.class);
		definition.addPropertyValue("type", className);
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
		beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
		beanDefinition.setPrimary(true);

		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, new String[0]);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
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

}
