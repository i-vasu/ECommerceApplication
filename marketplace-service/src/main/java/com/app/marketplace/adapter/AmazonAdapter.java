package com.app.marketplace.adapter;

import com.app.marketplace.dto.OrderDTO;
import com.app.marketplace.service.NormalizationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AmazonAdapter implements MarketplaceAdapter {

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
    public void handleWebhook(Map<String, Object> payload) {
        log.info("Received Amazon webhook event: {}", payload);
        // Verify signature
        // Process payload
        OrderDTO order = normalize(payload);
        log.info("Normalized Amazon Order: {}", order);
        // Dispatch event (to be implemented)
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
}
