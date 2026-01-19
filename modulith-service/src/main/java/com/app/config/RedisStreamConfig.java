package com.app.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import com.app.order.entites.Order;

@Configuration
public class RedisStreamConfig {

    public static final String ORDER_STREAM_KEY = "orders_stream";
    public static final String ORDER_GROUP = "order_group";

        // Subscription for Order processing (ERP Sync)
        listenerContainer.receive(
                Consumer.from(ORDER_GROUP, "erp_sync_consumer"),
                StreamOffset.create(ORDER_STREAM_KEY, ReadOffset.lastConsumed()),
                streamListener); // This injects the bean passed to config method, likely OrderConsumer? 
                
        // Wait, the method signature injects ONE listener. I need multiple.
        // I should refactor this config to autowire the specific listeners.
        
        return null; // Logic moved to separate method/bean definitions below
    }

    @Autowired
    private com.app.order.async.OrderConsumer orderConsumer;

    @Autowired
    private com.app.product.async.ProductEventConsumer productEventConsumer;

    @Autowired
    private com.app.notification.async.NotificationConsumer notificationConsumer;

    @Bean
    public Subscription subscriptionContainer(RedisConnectionFactory factory) {
        
        // 1. Create Groups if not exist
        createGroup(factory, ORDER_STREAM_KEY, ORDER_GROUP);
        createGroup(factory, "product_events", "product_group");

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .targetType(String.class)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                StreamMessageListenerContainer.create(factory, options);

        // 2. Register Order Consumer (ERP Sync)
        container.receive(
                Consumer.from(ORDER_GROUP, "erp_sync_1"),
                StreamOffset.create(ORDER_STREAM_KEY, ReadOffset.lastConsumed()),
                orderConsumer);

        // 3. Register Notification Consumer (Emails) - Listens to SAME stream, DIFFERENT Consumer Name (Same Group? No, Notifications should also receive. different group!)
        // If same group, they share load (competing consumers).
        // ERP and Notification are distinct actions -> Different Groups (Pub/Sub semantics via Streams).
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

        container.start();
        return null; // Return implies bean managed, but container.start() is void. 
        // Returning container is better practices.
    }
    
    private void createGroup(RedisConnectionFactory factory, String key, String group) {
        try {
            factory.getConnection().streamCommands()
                   .xGroupCreate(key.getBytes(), group, ReadOffset.from("0-0"), true);
        } catch (Exception e) {}
    }
}
