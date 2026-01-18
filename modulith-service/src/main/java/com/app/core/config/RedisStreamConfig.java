package com.app.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamListener;

import java.time.Duration;
import java.util.List;

/**
 * Centralized Redis Stream Configuration for the Modulith platform.
 * Handles event distribution across modules.
 */
@Configuration
public class RedisStreamConfig {

    public static final String PRODUCT_SYNC_STREAM = "product-sync-events";
    public static final String ORDER_EVENTS_STREAM = "order-events";
    public static final String BLOG_EVENTS_STREAM = "blog-events";

    @Autowired
    @Lazy
    private List<StreamListener<String, MapRecord<String, String, String>>> listeners;

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
                .create(connectionFactory, options);

        // Ensure Streams and Consumer Groups exist
        ensureGroup(connectionFactory, PRODUCT_SYNC_STREAM, "order-service-group");
        ensureGroup(connectionFactory, ORDER_EVENTS_STREAM, "order-service-group");
        ensureGroup(connectionFactory, BLOG_EVENTS_STREAM, "order-service-group");
        ensureGroup(connectionFactory, PRODUCT_SYNC_STREAM, "marketplace-group");
        ensureGroup(connectionFactory, ORDER_EVENTS_STREAM, "marketplace-group");

        container.start();
        return container;
    }

    private void ensureGroup(RedisConnectionFactory connectionFactory, String streamKey, String groupName) {
        try {
            connectionFactory.getConnection().streamCommands()
                    .xGroupCreate(streamKey.getBytes(), groupName, ReadOffset.from("0-0"), true);
        } catch (Exception e) {
            // Group already exists
        }
    }
}
