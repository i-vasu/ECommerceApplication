package com.app.core.services;

public interface PurchaseVerificationService {
    boolean hasPurchasedProduct(String email, Long productId);
}
