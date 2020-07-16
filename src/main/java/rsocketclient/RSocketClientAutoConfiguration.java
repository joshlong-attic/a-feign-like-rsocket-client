package rsocketclient;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Log4j2
@Configuration
@Import(RSocketClientsRegistrar.class)
class RSocketClientAutoConfiguration {

}
