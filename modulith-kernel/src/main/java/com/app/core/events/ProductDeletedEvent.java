package com.app.core.events;

/**
 * Domain event published when a product is removed from the catalog.
 */
public record ProductDeletedEvent(
    Long productId,
    String itemCode
) {}
