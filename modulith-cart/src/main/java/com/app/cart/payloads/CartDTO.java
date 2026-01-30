package com.app.cart.payloads;

import java.util.List;
import com.app.catalog.payloads.ProductDTO;

public record CartDTO(
        Long cartId,
        java.math.BigDecimal totalPrice,
        List<ProductDTO> products) {
    public CartDTO {
        if (totalPrice == null)
            totalPrice = java.math.BigDecimal.ZERO;
        if (products == null)
            products = List.of();
    }
}
