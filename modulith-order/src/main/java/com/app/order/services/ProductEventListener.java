package com.app.order.services;

import com.app.product.payloads.ProductSyncEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Service
public class ProductEventListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            var payload = message.getValue().get("payload");
            if (payload == null)
                return;

            var event = objectMapper.readValue(payload, ProductSyncEvent.class);

            log.info("Consumer: Received Product Sync Event via DragonflyDB (Stream). Status: {}",
                    event.getStatus());

        } catch (Exception e) {
            log.error("Failed to process stream message: {}", e.getMessage());
        }
    }
}
