package com.app.product.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductRedisConfig {
    public static final String PRODUCT_SYNC_STREAM = "product-sync-events";
}
