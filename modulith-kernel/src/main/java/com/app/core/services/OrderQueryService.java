package com.app.core.services;

import java.time.LocalDateTime;

/**
 * Service interface for querying order information.
 * Implemented by Order module, used by other modules for read-only access.
 */
public interface OrderQueryService {

    /**
     * Get order details for return processing.
     */
    OrderDetailsDTO getOrderDetails(Long orderId);

    /**
     * Count orders by email for fraud detection.
     */
    long countOrdersByEmail(String email);

    /**
     * DTO for order details (decoupled from Order entity).
     */
    record OrderDetailsDTO(
            Long orderId,
            String email,
            String status,
            LocalDateTime orderDate,
            LocalDateTime deliveredDate,
            Double totalAmount,
            Long paymentId) {
    }
}
