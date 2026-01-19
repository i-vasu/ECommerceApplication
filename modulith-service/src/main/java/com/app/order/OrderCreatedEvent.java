package com.app.order;

/**
 * Event published when a new order is successfully placed.
 * This is an internal Spring Modulith event.
 */
public record OrderCreatedEvent(
                Long orderId,
                String customerEmail,
                java.math.BigDecimal totalAmount) {
}
