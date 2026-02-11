package com.app.core.events;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Published when product data sync is completed from ERP system.
 * 
 * Publishers: Internal ERP service
 * Listeners: modulith-discovery (indexes products for search)
 * 
 * @param tenantId    Tenant for which sync was performed
 * @param itemCodes   List of item codes that were synced
 * @param syncedCount Number of items successfully synced
 * @param failedCount Number of items that failed to sync
 * @param syncTime    When sync was completed
 */
public record ProductSyncCompletedEvent(
        String tenantId,
        List<String> itemCodes,
        int syncedCount,
        int failedCount,
        LocalDateTime syncTime) {
    public ProductSyncCompletedEvent(String tenantId, List<String> itemCodes, int syncedCount, int failedCount) {
        this(tenantId, itemCodes, syncedCount, failedCount, LocalDateTime.now());
    }
}
