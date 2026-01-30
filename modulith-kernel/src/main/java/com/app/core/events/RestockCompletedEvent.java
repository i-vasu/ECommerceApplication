package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Published when restock order is completed in ERP system.
 * 
 * Publishers: modulith-erp-sync (ERPNextService)
 * Listeners: modulith-logistics (updates inventory levels)
 * 
 * @param productId        Product that was restocked
 * @param itemCode         ERP item code
 * @param orderedQuantity  Quantity that was ordered
 * @param receivedQuantity Quantity actually received
 * @param purchaseOrderId  ERP purchase order ID
 * @param completedAt      When restock was completed
 */
public record RestockCompletedEvent(
        Long productId,
        String itemCode,
        int orderedQuantity,
        int receivedQuantity,
        String purchaseOrderId,
        LocalDateTime completedAt) {
    public RestockCompletedEvent(Long productId, String itemCode, int orderedQuantity, int receivedQuantity,
            String purchaseOrderId) {
        this(productId, itemCode, orderedQuantity, receivedQuantity, purchaseOrderId, LocalDateTime.now());
    }
}
