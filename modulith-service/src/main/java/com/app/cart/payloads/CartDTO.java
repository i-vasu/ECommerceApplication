package com.app.cart.payloads;

import java.util.List;
import com.app.product.payloads.ProductDTO;

public record CartDTO(
        Long cartId,
        Double totalPrice,
        List<ProductDTO> products) {
    public CartDTO {
        if (totalPrice == null)
            totalPrice = 0.0;
        if (products == null)
            products = List.of();
    }
}
