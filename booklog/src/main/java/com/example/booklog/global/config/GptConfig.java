package com.example.booklog.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GptConfig {

    @Value("${spring.openai.secret-key}")
    private String secretKey;

    @Value("${spring.openai.model}")
    private String model;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getModel() {
        return model;
    }
}

