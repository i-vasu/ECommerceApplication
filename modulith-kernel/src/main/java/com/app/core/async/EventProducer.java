package com.app.core.async;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publishes an event to the specified Redis Stream.
     * 
     * @param streamKey The key of the stream (e.g., "orders_stream")
     * @param payload   The object to publish (will be serialized to JSON)
     */
    public void publish(String streamKey, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .ofObject(jsonPayload)
                    .withStreamKey(streamKey);

            redisTemplate.opsForStream().add(record);
            log.info(">>> Published Event to Stream [{}]: {}", streamKey, jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish event to stream [{}]: {}", streamKey, e.getMessage());
        }
    }

    /**
     * Publishes an event internally using Spring ApplicationEventPublisher.
     * Used for Spring Modulith cross-module communication.
     */
    public void publishEvent(Object event) {
        log.info(">>> Publishing Internal Event: {}", event);
        eventPublisher.publishEvent(event);
    }
}
