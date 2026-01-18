package com.app.order.services;

import com.app.product.payloads.ProductSyncEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductEventListener implements
        org.springframework.data.redis.stream.StreamListener<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductEventListener.class);

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public void onMessage(org.springframework.data.redis.connection.stream.MapRecord<String, String, String> message) {
        try {
            String payload = message.getValue().get("payload");
            if (payload == null)
                return;

            ProductSyncEvent event = objectMapper.readValue(payload, ProductSyncEvent.class);

            log.info(">>> Consumer: Received Product Sync Event via DragonflyDB (Stream). Status: {}",
                    event.getStatus());

        } catch (Exception e) {
            log.error("Failed to process stream message", e);
        }
    }
}
