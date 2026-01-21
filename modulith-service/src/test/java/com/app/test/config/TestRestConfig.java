package com.app.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class TestRestConfig {

    @Bean
    public RestTestClient restTestClient(WebApplicationContext context) {
        return RestTestClient.bindToApplicationContext(context).build();
    }
}
