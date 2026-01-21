package com.app.product.payloads;

import java.util.List;
import java.util.ArrayList;
import com.app.review.payloads.ProductReviewDTO;

public record ProductDTO(
        Long productId,
        String productName,
        String itemCode,
        String image,
        String description,
        Integer quantity,
        double price,
        double discount,
        double specialPrice,
        List<ProductVariantDTO> variants,
        List<ProductMediaDTO> media,
        List<ProductReviewDTO> reviews,
        Double averageRating) {

    // Compact constructor to handle null initialization
    public ProductDTO {
        variants = variants == null ? new ArrayList<>() : variants;
        media = media == null ? new ArrayList<>() : media;
        reviews = reviews == null ? new ArrayList<>() : reviews;
    }
}
