package com.app.marketplace.config;

import com.app.marketplace.listener.MarketplaceEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Configuration
public class RedisStreamConfig {

    public static final String PRODUCT_SYNC_STREAM = "product-sync-events";
    public static final String ORDER_EVENTS_STREAM = "order-events";
    public static final String CONSUMER_GROUP = "marketplace-group";

    @Autowired
    private MarketplaceEventListener marketplaceEventListener;

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
                .create(connectionFactory, options);

        try {
            connectionFactory.getConnection().streamCommands().xGroupCreate(
                    PRODUCT_SYNC_STREAM.getBytes(), CONSUMER_GROUP, ReadOffset.from("0-0"), true);
        } catch (Exception e) {
            // Group might already exist
        }
        try {
            connectionFactory.getConnection().streamCommands().xGroupCreate(
                    ORDER_EVENTS_STREAM.getBytes(), CONSUMER_GROUP, ReadOffset.from("0-0"), true);
        } catch (Exception e) {
            // Group might already exist
        }

        container.receive(
                org.springframework.data.redis.connection.stream.Consumer.from(CONSUMER_GROUP, "instance-1"),
                StreamOffset.create(PRODUCT_SYNC_STREAM, ReadOffset.lastConsumed()),
                marketplaceEventListener);

        container.receive(
                org.springframework.data.redis.connection.stream.Consumer.from(CONSUMER_GROUP, "instance-1"),
                StreamOffset.create(ORDER_EVENTS_STREAM, ReadOffset.lastConsumed()),
                marketplaceEventListener);

        container.start();
        return container;
    }
}
