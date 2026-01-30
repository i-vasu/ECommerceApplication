package com.app.core.events;

/**
 * Event to trigger a full product synchronization from ERPNext.
 */
public record ERPProductSyncRequestedEvent(
        String trigger,
        String tenantId,
        String erpNextUrl,
        String erpNextApiKey,
        String erpNextApiSecret) {
}
