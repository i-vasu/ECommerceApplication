package com.app.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {
    private Long variantId;
    private String color;
    private String size;
    private Integer stockQuantity;
}
