package com.app.core.events;

import java.math.BigDecimal;

/**
 * Published when a user views a product.
 */
public record ProductViewedEvent(
        Long productId,
        String email,
        BigDecimal price) {
    public ProductViewedEvent(Long productId, String email) {
        this(productId, email, null);
    }
}
