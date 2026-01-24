package com.app.core.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Event published when an order is successfully completed (paid + confirmed).
 * Used to track user purchase history for verified reviews.
 */
public record OrderCompletedEvent(
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("userId") Long userId,
        @JsonProperty("userEmail") String userEmail,
        @JsonProperty("productIds") List<Long> productIds,
        @JsonProperty("totalAmount") Double totalAmount) {
}
