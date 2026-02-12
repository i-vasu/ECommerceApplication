package com.app.review.services;

public interface PurchaseVerificationService {
    boolean isVerifiedPurchase(String email, Long productId);
}
