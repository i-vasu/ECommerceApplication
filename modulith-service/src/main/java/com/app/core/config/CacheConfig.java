package com.app.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Advanced Spring Cache Configuration
 * Features:
 * 1. JSON-based serialization for multi-environment compatibility
 * 2. Transaction awareness (prevents dirty cache reads)
 * 3. Graceful error handling (app continues if Redis is down)
 * 4. Granular TTL policies per cache name
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                ObjectMapper mapper = new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .activateDefaultTyping(mapper().getPolymorphicTypeValidator(),
                                                ObjectMapper.DefaultTyping.NON_FINAL);

                Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper,
                                Object.class);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                                .disableCachingNullValues();

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .transactionAware() // Ensure cache syncs with DB transactions
                                .withCacheConfiguration("products", defaultConfig.entryTtl(Duration.ofHours(1)))
                                .withCacheConfiguration("product-search",
                                                defaultConfig.entryTtl(Duration.ofMinutes(10)))
                                .withCacheConfiguration("users", defaultConfig.entryTtl(Duration.ofHours(24)))
                                .build();
        }

        @Override
        public CacheErrorHandler errorHandler() {
                // Log errors but don't crash the request (Cache-aside pattern)
                return new LoggingCacheErrorHandler();
        }

        private ObjectMapper mapper() {
                return new ObjectMapper().registerModule(new JavaTimeModule());
        }
}
