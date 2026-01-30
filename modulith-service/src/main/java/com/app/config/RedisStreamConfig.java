package com.app.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.beans.factory.annotation.Autowired;
import com.app.marketing.notification.async.NotificationConsumer;
import com.app.security.async.UserConsumer;

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

                StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                                .builder()
                                .pollTimeout(Duration.ofSeconds(1))
                                .targetType(String.class)
                                .build();

                StreamMessageListenerContainer<String, ObjectRecord<String, String>> container = StreamMessageListenerContainer
                                .create(factory, options);



                // 3. Register Notification Consumer (Emails)
                createGroup(factory, ORDER_STREAM_KEY, "notification_group");
                container.receive(
                                Consumer.from("notification_group", "email_sender_1"),
                                StreamOffset.create(ORDER_STREAM_KEY, ReadOffset.lastConsumed()),
                                notificationConsumer);

                createGroup(factory, "payment_events", "notification_group");
                container.receive(
                                Consumer.from("notification_group", "email_sender_2"),
                                StreamOffset.create("payment_events", ReadOffset.lastConsumed()),
                                notificationConsumer);



                // 5. Register User Consumer (ERP Sync)
                createGroup(factory, "user_events", "user_group");
                container.receive(
                                Consumer.from("user_group", "user_erp_sync_1"),
                                StreamOffset.create("user_events", ReadOffset.lastConsumed()),
                                userConsumer);



                // 7. Register Status Notification Consumer
                createGroup(factory, "order_status_events", "notification_group");
                container.receive(
                                Consumer.from("notification_group", "status_notifier_1"),
                                StreamOffset.create("order_status_events", ReadOffset.lastConsumed()),
                                notificationConsumer);

                container.start();
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

                container.start();
                return container;
        }

        @Autowired
        private UserConsumer userConsumer;

        private void createGroup(RedisConnectionFactory factory, String key, String group) {
                try {
                        factory.getConnection().streamCommands()
                                        .xGroupCreate(key.getBytes(), group, ReadOffset.from("0-0"), true);
                } catch (Exception e) {
                }
        }
}
