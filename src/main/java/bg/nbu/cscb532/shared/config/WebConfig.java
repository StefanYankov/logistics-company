package bg.nbu.cscb532.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Centralized configuration for Spring Web MVC.
 * This class is used to define global rules for web-related concerns like CORS.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures the application's Cross-Origin Resource Sharing (CORS) policy.
     * This is essential for allowing a decoupled frontend (like an Angular app)
     * to communicate with the backend API from a different origin (e.g., localhost:4200 -> localhost:8080).
     *
     * @param registry The Spring MVC CORS registry.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // TODO: change to the actual domain of the frontend app in production
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true);
    }
}
