package com.example.agriconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.example.agriconnect",
        "com.example.common",
        "com.example.Authentication",
})
@EnableJpaRepositories
public class AgriconnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriconnectApplication.class, args);
    }
}
