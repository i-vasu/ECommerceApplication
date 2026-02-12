package com.app.review.services;

import org.springframework.stereotype.Service;

@Service
public class NoOpPurchaseVerificationService implements PurchaseVerificationService {
    @Override
    public boolean isVerifiedPurchase(String email, Long productId) {
        // TODO: Implement actual verification logic (e.g., call Order Service via API/Event)
        return true;
    }
}
