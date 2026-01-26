package com.app.marketplace.listener;

import com.app.marketplace.adapter.AmazonAdapter;
import com.app.marketplace.adapter.FlipkartAdapter;
import com.app.marketplace.adapter.OndcAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MarketplaceEventListener implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceEventListener.class);

    @Autowired
    private AmazonAdapter amazonAdapter;

    @Autowired
    private FlipkartAdapter flipkartAdapter;

    @Autowired
    private OndcAdapter ondcAdapter;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String stream = message.getStream();
        Map<String, String> value = message.getValue();
        String payload = value.get("payload");

        try {
            if ("order-events".equals(stream)) {
                // Order Paid -> Stock Reduced -> Push to Amazon
                log.info("Processing Order Event in Marketplace: {}", payload);
                // Parse 'OrderPaidEvent' (we need the DTO class or just generic map)
                Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
                });
                Object orderId = event.get("orderId");
                // In real logic: fetch order items, get sku, decrement stock
                // For POC: just log
                mockUpdateAllMarketplaces("SKU-FROM-" + orderId, -1);

            } else if ("product-sync-events".equals(stream)) {
                // Stock Replenished -> Push to Amazon
                log.info("Processing Product Sync Event: {}", payload);
                // For 'SYNC_COMPLETE', we might fetch all stock and push.
                // For detailed events, we update specific SKU.
                mockUpdateAllMarketplaces("ALL-SKUS", 100);
            }
        } catch (Exception e) {
            log.error("Failed to process marketplace stream event", e);
        }
    }

    private void mockUpdateAllMarketplaces(String sku, int change) {
        amazonAdapter.updateInventory(sku, change);
        flipkartAdapter.updateInventory(sku, change);
        ondcAdapter.updateInventory(sku, change);
    }
}
