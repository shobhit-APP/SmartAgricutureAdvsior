package com.example.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up Swagger (OpenAPI) documentation.
 * <p>
 * This ensures that all controllers across modules (Agriconnect, Authentication, Common)
 * are exposed in a single Swagger UI interface.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Defines the OpenAPI specification details such as title, description,
     * version, and contact information.
     *
     * @return configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI agriconnectOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agriconnect Platform API")
                        .description("REST APIs for Agriconnect platform")
                        .version("1.0.0")
                        .contact(new Contact().name("Dev Team").email("dev@example.com")));
    }

    /**
     * Groups all APIs from different modules into one group for Swagger UI.
     *
     * @return configured {@link GroupedOpenApi} instance
     */
    @Bean
    public GroupedOpenApi agriconnectGroup() {
        return GroupedOpenApi.builder()
                .group("agriconnect-all")
                .packagesToScan(
                        "com.example.agriconnect.Controller",
                        "com.example.authentication.Controller",
                        "com.example.common.Controller")
                .build();
    }
}
