package com.app.config;

import com.app.services.ProductEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Configuration
public class RedisStreamConfig {

        public static final String PRODUCT_SYNC_STREAM = "product-sync-events";
        public static final String ORDER_EVENTS_STREAM = "order-events";
        public static final String BLOG_EVENTS_STREAM = "blog-events";
        public static final String CONSUMER_GROUP = "order-service-group";

        @Autowired
        private ProductEventListener productListener;

        @Autowired
        private com.app.services.OrderEventListener orderListener;

        @Autowired
        private com.app.listener.BlogEventListener blogListener;

        @Bean
        public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
                        RedisConnectionFactory connectionFactory) {

                StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                                .builder()
                                .pollTimeout(Duration.ofSeconds(1))
                                .build();

                StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
                                .create(connectionFactory, options);

                // Auto-create stream consumer group if needed
                createConsumerGroup(connectionFactory, PRODUCT_SYNC_STREAM, CONSUMER_GROUP);
                createConsumerGroup(connectionFactory, ORDER_EVENTS_STREAM, CONSUMER_GROUP);
                createConsumerGroup(connectionFactory, BLOG_EVENTS_STREAM, CONSUMER_GROUP);

                // Bind Product Listener
                container.receive(
                                Consumer.from(CONSUMER_GROUP, "instance-1"),
                                StreamOffset.create(PRODUCT_SYNC_STREAM, ReadOffset.lastConsumed()),
                                productListener);

                // Bind Order Listener (Handles ERPNext, Shipping, Email, Marketing)
                container.receive(
                                Consumer.from(CONSUMER_GROUP, "instance-1"),
                                StreamOffset.create(ORDER_EVENTS_STREAM, ReadOffset.lastConsumed()),
                                orderListener);

                // Bind Blog Listener
                container.receive(
                                Consumer.from(CONSUMER_GROUP, "instance-1"),
                                StreamOffset.create(BLOG_EVENTS_STREAM, ReadOffset.lastConsumed()),
                                blogListener);

                container.start();
                return container;
        }

        private void createConsumerGroup(RedisConnectionFactory connectionFactory, String streamKey, String groupName) {
                try {
                        connectionFactory.getConnection().streamCommands()
                                        .xGroupCreate(streamKey.getBytes(), groupName, ReadOffset.from("0-0"), true);
                } catch (Exception e) {
                        // Group already exists, ignore
                }
        }
}
