package com.example.common.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application-wide configuration class.
 * <p>
 * Provides reusable beans such as {@link RestTemplate} and {@link ObjectMapper}
 * that can be injected across modules.
 */
@Configuration
public class AppConfig {

    /**
     * Default constructor for AppConfig.
     */
    public AppConfig() {
        // default constructor
    }

    /**
     * Creates a {@link RestTemplate} bean for making HTTP requests
     * to external services.
     *
     * @return configured {@link RestTemplate} instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Creates a Jackson {@link ObjectMapper} bean for JSON serialization
     * and deserialization across the application.
     *
     * @return configured {@link ObjectMapper} instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
