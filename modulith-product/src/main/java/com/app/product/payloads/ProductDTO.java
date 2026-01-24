package com.app.product.payloads;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
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
        Double averageRating,
        Map<String, String> socialPulse,
        String scarcityMessage) {

    // Non-canonical constructor for backward compatibility with Mapstruct and
    // legacy code
    public ProductDTO(Long productId, String productName, String itemCode, String image, String description,
            Integer quantity, double price, double discount, double specialPrice,
            List<ProductVariantDTO> variants, List<ProductMediaDTO> media,
            List<ProductReviewDTO> reviews, Double averageRating) {
        this(productId, productName, itemCode, image, description, quantity, price, discount, specialPrice,
                variants, media, reviews, averageRating, new java.util.HashMap<>(), null);
    }

    // Compact constructor to handle null initialization
    public ProductDTO {
        variants = variants == null ? new ArrayList<>() : variants;
        media = media == null ? new ArrayList<>() : media;
        reviews = reviews == null ? new ArrayList<>() : reviews;
        socialPulse = socialPulse == null ? new HashMap<>() : socialPulse;
    }
}
