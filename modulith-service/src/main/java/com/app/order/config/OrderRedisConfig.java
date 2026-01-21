package com.app.order.config;

import com.app.order.order.OrderEventListener;
import com.app.order.services.ProductEventListener;
import com.app.marketing.services.BlogEventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class OrderRedisConfig {

        public static final String PRODUCT_SYNC_STREAM = "product-sync-events";
        public static final String ORDER_EVENTS_STREAM = "order-events";
        public static final String BLOG_EVENTS_STREAM = "blog-events";
        public static final String CONSUMER_GROUP = "order-service-group";

        private final ProductEventListener productListener;
        private final OrderEventListener orderListener;
        private final BlogEventListener blogListener;
        private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

        @PostConstruct
        public void registerOrderListeners() {
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
