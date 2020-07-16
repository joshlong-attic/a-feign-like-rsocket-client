package rsocketclient;

import com.example.rsocketfeign.client.RsocketFeignApplication;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

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


