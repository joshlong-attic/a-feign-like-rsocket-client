package com.joshlong.rsocket.client;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
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
		RSocketClientBuilder clientBuilder = this.context.getBean(RSocketClientBuilder.class);
		return clientBuilder.buildClientFor(this.type);
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
