package com.app.cart.payloads;

import java.math.BigDecimal;
import java.util.List;

public record CartDTO(
        Long cartId,
        Long userId,
        BigDecimal totalPrice,
        String couponCode,
        List<CartItemDTO> items) {
    
    public record CartItemDTO(
            Long cartItemId,
            Long productId,
            String productName,
            String itemCode,
            Integer quantity,
            BigDecimal productPrice,
            BigDecimal discount) {}

    public CartDTO {
        if (totalPrice == null)
            totalPrice = BigDecimal.ZERO;
        if (items == null)
            items = List.of();
    }
}
