package com.app.catalog.payloads;

import com.app.catalog.review.payloads.ProductReviewDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ProductDTO(
        Long productId,
        String productName,
        String itemCode,
        String image,
        String description,
        Integer quantity,
        java.math.BigDecimal price,
        java.math.BigDecimal discount,
        java.math.BigDecimal specialPrice,
        List<ProductVariantDTO> variants,
        List<ProductMediaDTO> media,
        List<ProductReviewDTO> reviews,
        Double averageRating,
        Map<String, String> socialPulse,
        String scarcityMessage,
        Map<String, String> sizeGuide,
        String materialStory,
        String stylistNotes,
        String modelMeasurements) {

    // 13-arg constructor for backward compatibility with legacy search services
    public ProductDTO(Long productId, String productName, String itemCode, String image, String description,
            Integer quantity, java.math.BigDecimal price, java.math.BigDecimal discount, java.math.BigDecimal specialPrice,
            List<ProductVariantDTO> variants, List<ProductMediaDTO> media,
            List<ProductReviewDTO> reviews, Double averageRating) {
        this(productId, productName, itemCode, image, description, quantity, price, discount, specialPrice,
                variants, media, reviews, averageRating, new java.util.HashMap<>(), null, null, null, null, null);
    }

    // 16-arg constructor for backward compatibility with visual/discovery services
    public ProductDTO(Long productId, String productName, String itemCode, String image, String description,
            Integer quantity, java.math.BigDecimal price, java.math.BigDecimal discount, java.math.BigDecimal specialPrice,
            List<ProductVariantDTO> variants, List<ProductMediaDTO> media,
            List<ProductReviewDTO> reviews, Double averageRating, Map<String, String> socialPulse,
            String scarcityMessage, Map<String, String> sizeGuide) {
        this(productId, productName, itemCode, image, description, quantity, price, discount, specialPrice,
                variants, media, reviews, averageRating, socialPulse, scarcityMessage, sizeGuide, null, null, null);
    }

    // Compact constructor to handle null initialization
    public ProductDTO {
        variants = variants == null ? new ArrayList<>() : variants;
        media = media == null ? new ArrayList<>() : media;
        reviews = reviews == null ? new ArrayList<>() : reviews;
        socialPulse = socialPulse == null ? new HashMap<>() : socialPulse;
    }
}
