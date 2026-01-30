package com.app.core.events;

import java.math.BigDecimal;

/**
 * Published when a user adds a product to their cart.
 */
public record AddToCartEvent(
        Long productId,
        String itemCode,
        BigDecimal price) {
}
