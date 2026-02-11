package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Published when inventory falls below threshold and restock is needed.
 * 
 * Publishers: modulith-logistics (InventoryOptimizationService)
 * Listeners: Internal ERP fulfillment service
 * 
 * @param productId         Product requiring restock
 * @param itemCode          ERP item code
 * @param requestedQuantity Quantity to order
 * @param currentQuantity   Current stock level
 * @param threshold         Restock threshold
 * @param reason            Reason for restock request
 * @param timestamp         When request was created
 */
public record RestockRequestedEvent(
        Long productId,
        String itemCode,
        int requestedQuantity,
        int currentQuantity,
        int threshold,
        String reason,
        java.time.LocalDateTime timestamp) {
    public RestockRequestedEvent(Long productId, String itemCode, int requestedQuantity, int currentQuantity,
            int threshold, String reason) {
        this(productId, itemCode, requestedQuantity, currentQuantity, threshold, reason, java.time.LocalDateTime.now());
    }
}
