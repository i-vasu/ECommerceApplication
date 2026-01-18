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
public class MarketplaceRedisConfig {

        public static final String PRODUCT_SYNC_STREAM = "product-sync-events";
        public static final String ORDER_EVENTS_STREAM = "order-events";
        public static final String CONSUMER_GROUP = "marketplace-group";

        @Autowired
        private MarketplaceEventListener marketplaceEventListener;

        @Autowired
        private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

        @jakarta.annotation.PostConstruct
        public void registerMarketplaceListeners() {
                container.receive(
                                org.springframework.data.redis.connection.stream.Consumer.from(CONSUMER_GROUP,
                                                "instance-1"),
                                StreamOffset.create(PRODUCT_SYNC_STREAM, ReadOffset.lastConsumed()),
                                marketplaceEventListener);

                container.receive(
                                org.springframework.data.redis.connection.stream.Consumer.from(CONSUMER_GROUP,
                                                "instance-1"),
                                StreamOffset.create(ORDER_EVENTS_STREAM, ReadOffset.lastConsumed()),
                                marketplaceEventListener);
        }
}
