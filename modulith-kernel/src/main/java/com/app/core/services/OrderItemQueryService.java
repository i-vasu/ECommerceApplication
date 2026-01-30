package com.app.core.services;

/**
 * Service interface for querying order item information.
 * Implemented by Order module, used by other modules for read-only access.
 */
public interface OrderItemQueryService {

    /**
     * Get order item details for return processing.
     */
    OrderItemDetailsDTO getOrderItemDetails(Long orderItemId);

    /**
     * DTO for order item details (decoupled from OrderItem entity).
     */
    record OrderItemDetailsDTO(
            Long orderItemId,
            Long orderId,
            String productName,
            String itemCode,
            Integer quantity,
            Integer returnedQuantity,
            Double orderedPrice,
            Double discount,
            String status) {
    }
}
