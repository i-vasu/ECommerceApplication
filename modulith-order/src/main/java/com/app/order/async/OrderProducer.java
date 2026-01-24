package com.app.order.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderProducer {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void sendOrder(Long orderId) {
        try {
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .ofObject(String.valueOf(orderId))
                    .withStreamKey("order-events");

            redisTemplate.opsForStream().add(record);
            System.out.println(">>> Queued Order ID: " + orderId + " to DragonflyDB Stream");
        } catch (Exception e) {
            System.err.println("Failed to queue order: " + e.getMessage());
            // Fallback: log to DB or File
        }
    }
}
