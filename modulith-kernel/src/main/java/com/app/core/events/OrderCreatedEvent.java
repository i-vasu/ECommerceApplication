package com.app.core.events;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain event published when an order is successfully placed.
 */
public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String email,
        BigDecimal totalAmount,
        List<OrderItemData> items,
        java.time.LocalDateTime createdAt) {

    public record OrderItemData(String itemCode, Integer quantity, BigDecimal price) {
    }

    public OrderCreatedEvent(Long orderId, Long userId, String email, BigDecimal totalAmount,
            List<OrderItemData> items) {
        this(orderId, userId, email, totalAmount, items, java.time.LocalDateTime.now());
    }
}
