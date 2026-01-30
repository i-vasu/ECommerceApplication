package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Published when inventory falls below critical threshold.
 * 
 * Publishers: modulith-logistics (InventoryService)
 * Listeners: modulith-order (check affected orders), modulith-catalog (update
 * availability)
 * 
 * @param itemCode        Item code
 * @param currentQuantity Current stock level
 * @param threshold       Critical threshold
 * @param warehouse       Warehouse location
 * @param timestamp       When threshold was crossed
 */
public record InventoryLowEvent(
        String itemCode,
        int currentQuantity,
        int threshold,
        String warehouse,
        LocalDateTime timestamp) {
    public InventoryLowEvent(String itemCode, int currentQuantity, int threshold, String warehouse) {
        this(itemCode, currentQuantity, threshold, warehouse, LocalDateTime.now());
    }
}
