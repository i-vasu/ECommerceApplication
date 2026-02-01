package com.app.catalog.payloads;

public record ProductVariantDTO(Long variantId, String itemCode, String color, String size, Integer stockQuantity) {
}
