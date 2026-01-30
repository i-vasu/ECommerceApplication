package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Lightweight event to trigger status updates in the Order module.
 */
public record OrderStatusEvent(
        Long orderId,
        String email,
        String status,
        String awbNumber,
        String carrier,
        String tenantId,
        LocalDateTime timestamp) {

    public OrderStatusEvent(Long orderId, String email, String status, String awbNumber, String carrier,
            String tenantId) {
        this(orderId, email, status, awbNumber, carrier, tenantId, LocalDateTime.now());
    }

    public OrderStatusEvent(Long orderId, String status) {
        this(orderId, null, status, null, null, null, LocalDateTime.now());
    }
}
