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
public class FlipkartAdapter implements MarketplaceAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FlipkartAdapter.class);

    private final NormalizationEngine normalizationEngine;
    private final String flipkartKey = "FLIPKART_KEY";

    @Override
    public List<OrderDTO> fetchOrders() {
        log.info("Fetching orders from Flipkart API");
        return Collections.emptyList();
    }

    @Override
    public OrderDTO handleWebhook(Map<String, Object> payload) {
        log.info("Received Flipkart webhook event: {}", payload);
        OrderDTO order = normalize(payload);
        log.info("Normalized Flipkart Order: {}", order);
        return order;
    }

    @Override
    public OrderDTO normalize(Object payload) {
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            return normalizationEngine.mapToInternal("FLIPKART", map);
        }
        throw new IllegalArgumentException("Invalid payload type for Flipkart adapter");
    }

    @Override
    public void updateInventory(String sku, int quantity) {
        log.info("Mock: Updating stock on Flipkart for SKU: {} to Qty: {}", sku, quantity);
    }
}
