package com.app.core.events;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event to request ERPNext synchronization.
 * Consumed by Admin module which owns ERPNext integration.
 */
public record ERPNextSyncRequestEvent(
        @JsonProperty("entityType") String entityType, // "USER", "ORDER", "PRODUCT"
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("action") String action // "CREATE", "UPDATE", "DELETE"
) {
}
