package com.app.core.events;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain event published when a product's details are updated.
 */
public record ProductUpdatedEvent(
        Long productId,
        String itemCode,
        String productName,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        Integer oldQuantity,
        Integer newQuantity,
        String imageUrl,
        List<String> tags) {
}
