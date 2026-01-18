package com.app.marketplace.adapter;

import com.app.order.payloads.OrderDTO;
import com.app.marketplace.service.NormalizationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AmazonAdapter implements MarketplaceAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AmazonAdapter.class);

    private final NormalizationEngine normalizationEngine;

    // In a real scenario, this would be injected value
    private final String spApiSecret = "AMAZON_SECRET";

    @Override
    public List<OrderDTO> fetchOrders() {
        log.info("Fetching orders from Amazon SP-API");
        // Implementation to call Amazon SP-API would go here
        return Collections.emptyList();
    }

    @Override
    public OrderDTO handleWebhook(Map<String, Object> payload) {
        log.info("Received Amazon webhook event: {}", payload);
        // Verify signature (would be done in Controller or here)
        // Process payload
        OrderDTO order = normalize(payload);
        log.info("Normalized Amazon Order: {}", order);
        return order;
    }

    @Override
    public OrderDTO normalize(Object payload) {
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            return normalizationEngine.mapToInternal("AMAZON", map);
        }
        throw new IllegalArgumentException("Invalid payload type for Amazon adapter");
    }

    @Override
    public void updateInventory(String sku, int quantity) {
        log.info("Mock: Updating stock on Amazon for SKU: {} to Qty: {}", sku, quantity);
        // Call Amazon SP-API Feeds API or Inventory API
    }
}
