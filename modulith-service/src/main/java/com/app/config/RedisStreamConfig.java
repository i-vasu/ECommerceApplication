package com.app.config;

import com.app.marketing.notification.async.NotificationConsumer;
import com.app.security.async.UserConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Configuration
public class RedisStreamConfig {

        public static final String ORDER_STREAM_KEY = "orders_stream";
        public static final String ORDER_GROUP = "order_group";





        @Autowired
        private NotificationConsumer notificationConsumer;

        @Bean
        public StreamMessageListenerContainer<String, ObjectRecord<String, String>> streamMessageListenerContainer(
                        RedisConnectionFactory factory) {

                // 1. Create Groups if not exist
                createGroup(factory, ORDER_STREAM_KEY, ORDER_GROUP);
                createGroup(factory, "product_events", "product_group");
                createGroup(factory, "payment_events", "notification_group");
                createGroup(factory, "user_events", "user_group");
                createGroup(factory, "order_status_events", "notification_group");
                createGroup(factory, ORDER_STREAM_KEY, "notification_group");

                StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                                .builder()
                                .pollTimeout(Duration.ofSeconds(1))
                                .targetType(String.class)
                                .build();

                StreamMessageListenerContainer<String, ObjectRecord<String, String>> container = StreamMessageListenerContainer
                                .create(factory, options);



                // 3. Register Notification Consumer (Emails)
                container.receive(
                                Consumer.from("notification_group", "email_sender_1"),
                                StreamOffset.create(ORDER_STREAM_KEY, ReadOffset.lastConsumed()),
                                notificationConsumer);

                container.receive(
                                Consumer.from("notification_group", "email_sender_2"),
                                StreamOffset.create("payment_events", ReadOffset.lastConsumed()),
                                notificationConsumer);

                container.receive(
                                Consumer.from("notification_group", "email_sender_3"),
                                StreamOffset.create("order_status_events", ReadOffset.lastConsumed()),
                                notificationConsumer);



                // 5. Register User Consumer (ERP Sync)
                container.receive(
                                Consumer.from("user_group", "user_erp_sync_1"),
                                StreamOffset.create("user_events", ReadOffset.lastConsumed()),
                                userConsumer);



                // 7. Register Status Notification Consumer
                container.receive(
                                Consumer.from("notification_group", "status_notifier_1"),
                                StreamOffset.create("order_status_events", ReadOffset.lastConsumed()),
                                notificationConsumer);

                return container;
        }

        @Bean
        public StreamMessageListenerContainer<String, MapRecord<String, String, String>> mapStreamMessageListenerContainer(
                        RedisConnectionFactory factory) {

                createGroup(factory, "product-sync-events", "marketplace-group");
                createGroup(factory, "order-events", "marketplace-group");

                StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                                .builder()
                                .pollTimeout(Duration.ofSeconds(1))
                                .build();

                StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
                                .create(factory, options);

                return container;
        }

        @Autowired
        private UserConsumer userConsumer;

        private void createGroup(RedisConnectionFactory factory, String key, String group) {
                try (RedisConnection connection = factory.getConnection()) {
                        connection.streamCommands()
                                        .xGroupCreate(key.getBytes(StandardCharsets.UTF_8), group, ReadOffset.from("0-0"), true);
                        log.info("Created consumer group {} for stream {}", group, key);
                } catch (RedisSystemException e) {
                        if (e.getMessage() != null && (e.getMessage().contains("BUSYGROUP") || e.getMessage().contains("already exists"))) {
                                log.debug("Consumer group {} already exists for stream {}", group, key);
                        } else {
                                log.error("Failed to create consumer group {} for stream {}: {}", group, key, e.getMessage());
                        }
                } catch (Exception e) {
                        log.error("Unexpected error creating consumer group {} for stream {}: {}", group, key, e.getMessage());
                }
        }
}
