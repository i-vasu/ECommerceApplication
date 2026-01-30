package com.app.core.events;

import java.io.Serializable;
import java.util.List;

/**
 * Event published when AI enrichment is complete for a product.
 * Listened by: modulith-catalog (to update product metadata)
 */
public record ProductEnrichedEvent(
        Long productId,
        String itemCode,
        List<String> aiTags) implements Serializable {
}
