package com.app.core.events;

import java.io.Serializable;
import java.util.List;

/**
 * Event published after order module validates a return request.
 * Listened by: modulith-support (to create return record)
 */
public record ReturnValidatedEvent(
        Long orderId,
        String email,
        String reason,
        String refundType,
        double totalRefundAmount,
        List<ValidatedReturnItem> items) implements Serializable {
    public record ValidatedReturnItem(Long orderItemId, String itemCode, Integer quantity, Double unitRefundAmount) {
    }
}
