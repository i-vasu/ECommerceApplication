package com.app.review.controllers;

import com.app.review.payloads.ProductReviewDTO;
import com.app.review.services.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ProductReviewDTO> addReview(@PathVariable Long productId,
            @RequestBody ProductReviewDTO reviewDTO) {
        ProductReviewDTO savedReview = reviewService.addReview(productId, reviewDTO);
        return new ResponseEntity<>(savedReview, HttpStatus.CREATED);
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<ProductReviewDTO>> getReviewsByProduct(@PathVariable Long productId) {
        List<ProductReviewDTO> reviews = reviewService.getReviewsByProduct(productId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<String> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return new ResponseEntity<>("Review deleted successfully", HttpStatus.OK);
    }
}
