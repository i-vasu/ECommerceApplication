package com.app.core.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EventProducer {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Publishes an event to the specified Redis Stream.
     * @param streamKey The key of the stream (e.g., "orders_stream")
     * @param payload The object to publish (will be serialized to JSON)
     */
    public void publish(String streamKey, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .ofObject(jsonPayload)
                    .withStreamKey(streamKey);

            redisTemplate.opsForStream().add(record);
            System.out.println(">>> Published Event to Stream [" + streamKey + "]: " + jsonPayload);
        } catch (Exception e) {
            System.err.println("Failed to publish event to stream [" + streamKey + "]: " + e.getMessage());
            // In prod, consider fallback or throwing exception
        }
    }
}
