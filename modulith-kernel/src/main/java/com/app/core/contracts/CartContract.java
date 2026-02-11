package com.app.core.contracts;

import java.math.BigDecimal;
import java.util.List;

/**
 * UBIQUITOUS LANGUAGE: Cart Contract.
 * Decouples the Checkout and Order modules from the JPA-specific Cart entity.
 * Uses Records for immutable, cross-module data safety.
 */
public record CartContract(
    Long cartId,
    Long userId,
    BigDecimal subTotal,
    String couponCode,
    List<CartItemContract> items
) {
    public record CartItemContract(
        Long productId,
        String itemCode,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal discount
    ) {}
}
