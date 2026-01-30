package com.app.core.events;

import java.io.Serializable;
import java.util.Map;

/**
 * Event published when a new or updated item is received from ERPNext via
 * webhook.
 * Listened by: modulith-catalog (to update product data)
 */
public record ERPItemSyncRequestedEvent(
        Map<String, Object> itemData) implements Serializable {
}
