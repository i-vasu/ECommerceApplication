package com.app.product.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record ProductVariantDTO(Long variantId, String itemCode, String color, String size, Integer stockQuantity) {
}
