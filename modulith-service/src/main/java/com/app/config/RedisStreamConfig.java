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

@Configuration
public class RedisStreamConfig {

        public static final String ORDER_STREAM_KEY = "orders_stream";
        public static final String ORDER_GROUP = "order_group";

        @Autowired
        private com.app.order.async.OrderConsumer orderConsumer;

        @Autowired
        private com.app.product.async.ProductEventConsumer productEventConsumer;

        @Autowired
        private com.app.notification.async.NotificationConsumer notificationConsumer;

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

                // 2. Register Order Consumer (ERP Sync)
                container.receive(
                                Consumer.from(ORDER_GROUP, "erp_sync_1"),
                                StreamOffset.create(ORDER_STREAM_KEY, ReadOffset.lastConsumed()),
                                orderConsumer);

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

                // 4. Register Product Consumer
                container.receive(
                                Consumer.from("product_group", "product_processor_1"),
                                StreamOffset.create("product_events", ReadOffset.lastConsumed()),
                                productEventConsumer);

                // 5. Register User Consumer (ERP Sync)
                createGroup(factory, "user_events", "user_group");
                container.receive(
                                Consumer.from("user_group", "user_erp_sync_1"),
                                StreamOffset.create("user_events", ReadOffset.lastConsumed()),
                                userConsumer);

                // 6. Register Cancellation Consumer
                createGroup(factory, "cancellation_events", "cancellation_group");
                container.receive(
                                Consumer.from("cancellation_group", "cancel_worker_1"),
                                StreamOffset.create("cancellation_events", ReadOffset.lastConsumed()),
                                orderCancellationConsumer);

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
        public StreamMessageListenerContainer<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> mapStreamMessageListenerContainer(
                        RedisConnectionFactory factory) {

                createGroup(factory, "product-sync-events", "marketplace-group");
                createGroup(factory, "order-events", "marketplace-group");

                StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                                .builder()
                                .pollTimeout(Duration.ofSeconds(1))
                                .build();

                StreamMessageListenerContainer<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> container = StreamMessageListenerContainer
                                .create(factory, options);

                container.start();
                return container;
        }

        @Autowired
        private com.app.identity.async.UserConsumer userConsumer;

        @Autowired
        private com.app.order.async.OrderCancellationConsumer orderCancellationConsumer;

        private void createGroup(RedisConnectionFactory factory, String key, String group) {
                try {
                        factory.getConnection().streamCommands()
                                        .xGroupCreate(key.getBytes(), group, ReadOffset.from("0-0"), true);
                } catch (Exception e) {
                }
        }
}
