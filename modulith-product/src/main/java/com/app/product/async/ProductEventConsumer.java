package com.app.product.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.app.product.payloads.ProductSyncEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ProductEventConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            String jsonPayload = message.getValue();
            ProductSyncEvent event = objectMapper.readValue(jsonPayload, ProductSyncEvent.class);

            System.out.println("Processing Product Event: " + event.getStatus() + " for Product ID: " + event.getProductId());

            // Logic:
            // 1. If we had an external search engine (Elasticsearch), this is where we'd sync.
            // 2. Since we use ParadeDB (Postgres), the data is already in DB.
            // 3. We can use this to Invalidate Cache (if not done by Service) or trigger other workflows.
            
            // Example workflow: Notify Analytics or Recommendations engine
            
        } catch (Exception e) {
            System.err.println("Error processing product event: " + e.getMessage());
        }
    }
}
