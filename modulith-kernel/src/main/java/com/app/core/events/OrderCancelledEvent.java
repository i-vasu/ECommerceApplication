package com.app.core.events;

/**
 * Domain event published when an order is cancelled.
 * Decouples order cancellation from external systems (Inventory, Payment,
 * Logistics).
 */
public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        Double totalAmount,
        String reason,
        java.util.List<CancelledItem> items,
        java.time.LocalDateTime cancelledAt) {
    public record CancelledItem(String itemCode, Integer quantity) {
    }

    public OrderCancelledEvent(Long orderId, Long userId, Double totalAmount, String reason,
            java.util.List<CancelledItem> items) {
        this(orderId, userId, totalAmount, reason, items, java.time.LocalDateTime.now());
    }
}
