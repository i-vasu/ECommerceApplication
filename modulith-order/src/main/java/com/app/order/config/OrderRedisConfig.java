package com.app.order.config;

import com.app.order.services.ProductEventListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class OrderRedisConfig {

        public static final String PRODUCT_SYNC_STREAM = "product-sync-events";
        public static final String ORDER_EVENTS_STREAM = "order-events";
        public static final String BLOG_EVENTS_STREAM = "blog-events";
        public static final String CONSUMER_GROUP = "order-service-group";

        private final ProductEventListener productListener;
        private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
        private final RedisConnectionFactory factory;

        @PostConstruct
        public void registerOrderListeners() {
                // Ensure Consumer Group exists
                createConsumerGroup(factory, PRODUCT_SYNC_STREAM, CONSUMER_GROUP);

                // Bind Product Listener
                container.receive(
                                Consumer.from(CONSUMER_GROUP, "instance-1"),
                                StreamOffset.create(PRODUCT_SYNC_STREAM, ReadOffset.lastConsumed()),
                                productListener);
        }

        private void createConsumerGroup(RedisConnectionFactory connectionFactory, String streamKey, String groupName) {
                try (RedisConnection connection = connectionFactory.getConnection()) {
                        connection.streamCommands()
                                        .xGroupCreate(streamKey.getBytes(StandardCharsets.UTF_8), groupName, ReadOffset.from("0-0"), true);
                        log.info("Created consumer group {} for stream {}", groupName, streamKey);
                } catch (RedisSystemException e) {
                        if (e.getMessage() != null && (e.getMessage().contains("BUSYGROUP") || e.getMessage().contains("already exists"))) {
                                log.debug("Consumer group {} already exists for stream {}", groupName, streamKey);
                        } else {
                                log.error("Failed to create consumer group {} for stream {}: {}", groupName, streamKey, e.getMessage());
                        }
                } catch (Exception e) {
                        log.error("Unexpected error creating consumer group {} for stream {}: {}", groupName, streamKey, e.getMessage());
                }
        }
}
