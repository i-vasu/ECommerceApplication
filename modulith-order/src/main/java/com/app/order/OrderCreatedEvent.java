package com.app.order;

import java.math.BigDecimal;

/**
 * Event published when a new order is successfully placed.
 * This is an internal Spring Modulith event.
 */
public record OrderCreatedEvent(
        Long orderId,
        String customerEmail,
        BigDecimal totalAmount) {
}
