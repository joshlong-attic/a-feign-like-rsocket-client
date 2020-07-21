package com.joshlong.rsocket.client.metadata;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.codec.StringDecoder;
import org.springframework.util.MimeType;

import javax.annotation.PostConstruct;

@Log4j2
@Profile("service")
@Configuration
@EnableAutoConfiguration
class RSocketServerConfiguration {

	@Bean
	RSocketStrategiesCustomizer rSocketStrategiesCustomizer() {
		return strategies -> strategies//
				.metadataExtractorRegistry(registry -> {
					registry.metadataToExtract(Constants.CLIENT_ID_MIME_TYPE, String.class, Constants.CLIENT_ID_HEADER);
					registry.metadataToExtract(Constants.LANG_MIME_TYPE, String.class, Constants.LANG_HEADER);
				})//
				.decoders(decoders -> decoders.add(StringDecoder.allMimeTypes()));
	}

	@Bean
	GreetingsController greetingsController() {
		return new GreetingsController();
	}

	@PostConstruct
	public void start() {
		log.info("starting " + RSocketServerConfiguration.class.getName() + '.');
	}

}

class Constants {

	public static final String CLIENT_ID_HEADER = "client-id";

	public static final String LANG_HEADER = "lang";

	public static final String CLIENT_ID_MIME_TYPE_VALUE = "messaging/x.bootiful." + CLIENT_ID_HEADER;

	public static final String LANG_MIME_TYPE_VALUE = "messaging/x.bootiful." + LANG_HEADER;

	public static final MimeType CLIENT_ID_MIME_TYPE = MimeType.valueOf(CLIENT_ID_MIME_TYPE_VALUE);

	public static final MimeType LANG_MIME_TYPE = MimeType.valueOf(LANG_MIME_TYPE_VALUE);

}
