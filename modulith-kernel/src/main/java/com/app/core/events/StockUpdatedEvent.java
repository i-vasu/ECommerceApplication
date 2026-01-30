package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Published when stock quantity is updated in the system.
 * 
 * Publishers: modulith-erp-sync (during sync), modulith-logistics (during
 * reservation)
 * Listeners: modulith-catalog (updates product cache), modulith-discovery
 * (updates search index)
 * 
 * @param itemCode    ERP item code
 * @param oldQuantity Previous quantity
 * @param newQuantity Current quantity
 * @param source      Source of update (ERP, MANUAL, RESERVATION, PURCHASE)
 * @param updatedAt   When stock was updated
 */
public record StockUpdatedEvent(
        String itemCode,
        int oldQuantity,
        int newQuantity,
        String source,
        LocalDateTime updatedAt) {
    public StockUpdatedEvent(String itemCode, int oldQuantity, int newQuantity, String source) {
        this(itemCode, oldQuantity, newQuantity, source, LocalDateTime.now());
    }
}
