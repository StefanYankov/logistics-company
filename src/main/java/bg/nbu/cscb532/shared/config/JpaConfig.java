package bg.nbu.cscb532.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


/**
 * Configuration class to enable Spring Data JPA auditing features.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
