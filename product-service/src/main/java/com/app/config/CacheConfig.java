package com.app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * Spring Cache Configuration
 * Enables declarative caching with @Cacheable, @CacheEvict, etc.
 */
@Configuration
@EnableCaching
public class CacheConfig {

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                // Default cache config (30 minutes TTL)
                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .disableCachingNullValues();

                // Product cache (30 minutes)
                RedisCacheConfiguration productConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .disableCachingNullValues();

                // Search results cache (5 minutes)
                RedisCacheConfiguration searchConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .disableCachingNullValues();

                // User session cache (1 hour)
                RedisCacheConfiguration userConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1))
                                .disableCachingNullValues();

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withCacheConfiguration("products", productConfig)
                                .withCacheConfiguration("product-search", searchConfig)
                                .withCacheConfiguration("users", userConfig)
                                .build();
        }
}
