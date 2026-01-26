package com.app.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HyperswitchConfig {

    @Value("${hyperswitch.api.key:}")
    private String apiKey;

    @Bean(name = "hyperswitchRestClient")
    public RestClient hyperswitchRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.hyperswitch.io")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("api-key", apiKey)
                .build();
    }
}
