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
public class OndcAdapter implements MarketplaceAdapter {

    private final NormalizationEngine normalizationEngine;

    @Override
    public List<OrderDTO> fetchOrders() {
        // ONDC is typically push-based (webhooks/callbacks), so pulling might be
        // different or not applicable
        return Collections.emptyList();
    }

    @Override
    public OrderDTO handleWebhook(Map<String, Object> payload) {
        log.info("Received ONDC webhook event: {}", payload);
        // ONDC Beckn protocol handling
        // 1. Verify header (Authorization)
        // 2. Process 'on_confirm' or 'on_status'
        OrderDTO order = normalize(payload);
        log.info("Normalized ONDC Order: {}", order);
        return order;
    }

    @Override
    public OrderDTO normalize(Object payload) {
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            return normalizationEngine.mapToInternal("ONDC", map);
        }
        throw new IllegalArgumentException("Invalid payload type for ONDC adapter");
    }
}
