package com.example.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared OpenAPI configuration placed in common module so all controllers
 * across modules (agriconnect, authentication, common) are exposed in one UI.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI agriconnectOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agriconnect Platform API")
                        .description("REST APIs for Agriconnect platform")
                        .version("1.0.0")
                        .contact(new Contact().name("Dev Team").email("dev@example.com")));
    }

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
