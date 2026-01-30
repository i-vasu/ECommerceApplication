package com.app.core.events;

import java.io.Serializable;
import java.util.Map;

/**
 * Event published when a user requests a return.
 * Listened by: modulith-order (for validation and calculation)
 */
public record ReturnRequestedEvent(
        Long orderId,
        String email,
        Map<Long, Integer> itemsToReturn,
        String reason,
        String refundType) implements Serializable {
}
