package com.app.order.services;

import com.app.catalog.payloads.ProductSyncEvent;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Service
public class ProductEventListener implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LogManager.getLogger(ProductEventListener.class);

    private final ObjectMapper objectMapper;

    @Override
    @Observed(name = "event.consumer", contextualName = "consume-product-sync-event")
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
