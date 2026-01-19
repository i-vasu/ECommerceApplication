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

    @Bean
    public Subscription orderSubscription(RedisConnectionFactory factory,
                                          StreamListener<String, ObjectRecord<String, String>> streamListener) {
                                          
        try {
            // Ensure group exists (catch exception if already exists)
            factory.getConnection().streamCommands()
                   .xGroupCreate(ORDER_STREAM_KEY.getBytes(), ORDER_GROUP, ReadOffset.from("0-0"), true);
        } catch (Exception e) {
            // Group already exists, ignore
        }

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .targetType(String.class)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> listenerContainer =
                StreamMessageListenerContainer.create(factory, options);

        Subscription subscription = listenerContainer.receive(
                Consumer.from(ORDER_GROUP, "consumer_1"),
                StreamOffset.create(ORDER_STREAM_KEY, ReadOffset.lastConsumed()),
                streamListener);

        listenerContainer.start();
        return subscription;
    }
}
