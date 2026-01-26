package com.app.core.events;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when a new order is created.
 * Consumed by Inventory module to finalize stock deduction.
 */
public record OrderCreatedEvent(
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("userId") Long userId,
        @JsonProperty("totalAmount") Double totalAmount) {
}
