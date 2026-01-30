package com.app.core.events;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain event published when a new product is added to the catalog.
 */
public record ProductCreatedEvent(
        Long productId,
        String itemCode,
        String productName,
        BigDecimal price,
        Integer quantity,
        String imageUrl,
        List<String> tags) {
}
