package com.app.services;

import com.app.payloads.ProductSyncEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@lombok.extern.slf4j.Slf4j
public class ProductEventListener implements
        org.springframework.data.redis.stream.StreamListener<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> {

    @Autowired
    private ERPNextService erpNextService;

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

            if ("SYNC_COMPLETE".equalsIgnoreCase(event.getStatus())) {
                log.info(">>> Consumer: Triggering local Meilisearch re-indexing...");
                erpNextService.syncProductsFromERPNext();
            }
        } catch (Exception e) {
            log.error("Failed to process stream message", e);
        }
    }
}
