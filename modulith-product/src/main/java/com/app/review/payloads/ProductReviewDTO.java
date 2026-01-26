package com.app.review.payloads;

import java.time.LocalDateTime;

public record ProductReviewDTO(
                Long reviewId,
                Long userId,
                String userName,
                String email,
                int rating,
                String comment,
                boolean isVerifiedPurchase,
                LocalDateTime createdAt) {
}
