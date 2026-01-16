package com.app.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisStreamConfig {
    public static final String PRODUCT_SYNC_STREAM = "product-sync-events";
}
