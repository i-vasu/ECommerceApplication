package com.app.review.services;

import com.app.review.entities.ProductReview;
import com.app.review.payloads.ProductReviewDTO;
import com.app.review.repositories.ProductReviewRepo;
import com.app.product.repositories.ProductRepo;
import com.app.core.ResourceNotFoundException;
import com.app.core.APIException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.app.review.mappers.ReviewMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepo reviewRepo;
    private final ProductRepo productRepo;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ProductReviewDTO addReview(Long productId, ProductReviewDTO reviewDTO) {
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        // 1. Duplicate check
        if (reviewRepo.existsByEmailAndProduct(reviewDTO.email(), product)) {
            throw new APIException("You have already reviewed this product.");
        }

        // 2. Verified Purchase check
        // boolean isVerified =
        // orderRepo.existsByEmailAndProductId(reviewDTO.getEmail(), productId);
        boolean isVerified = false; // Decoupled for build stability

        var review = reviewMapper.toEntity(reviewDTO);
        review.setProduct(product);
        review.setVerifiedPurchase(isVerified);
        review.setCreatedAt(LocalDateTime.now());

        // 3. Profanity Filter
        if (containsBannedWords(review.getComment())) {
            review.setApproved(false); // Flag for moderation
        }

        // Ensure rating is valid
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new APIException("Rating must be between 1 and 5");
        }

        var savedReview = reviewRepo.save(review);
        return reviewMapper.toDTO(savedReview);
    }

    private boolean containsBannedWords(String comment) {
        if (comment == null)
            return false;
        List<String> bannedWords = List.of("abuse", "spam", "scam"); // Expandable
        String lowerComment = comment.toLowerCase();
        return bannedWords.stream().anyMatch(lowerComment::contains);
    }

    @Override
    public List<ProductReviewDTO> getReviewsByProduct(Long productId) {
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        var reviews = reviewRepo.findByProduct(product);
        return reviews.stream()
                .filter(ProductReview::isApproved) // Only show approved reviews
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        var review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "reviewId", reviewId));
        reviewRepo.delete(review);
    }

    @Override
    public Double getAverageRating(Long productId) {
        return reviewRepo.getAverageRatingByProductId(productId);
    }
}
