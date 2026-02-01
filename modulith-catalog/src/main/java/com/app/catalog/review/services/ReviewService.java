package com.app.catalog.review.services;

import com.app.catalog.review.payloads.ProductReviewDTO;

import java.util.List;

public interface ReviewService {

    ProductReviewDTO addReview(Long productId, ProductReviewDTO reviewDTO);

    List<ProductReviewDTO> getReviewsByProduct(Long productId);

    void deleteReview(Long reviewId);

    Double getAverageRating(Long productId);
}
