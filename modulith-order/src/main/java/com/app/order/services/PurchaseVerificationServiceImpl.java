package com.app.order.services;

import com.app.core.services.PurchaseVerificationService;
import com.app.order.repositories.OrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PurchaseVerificationServiceImpl implements PurchaseVerificationService {

    private final OrderRepo orderRepo;

    @Override
    public boolean hasPurchasedProduct(String email, Long productId) {
        return orderRepo.existsByEmailAndProductId(email, productId);
    }
}
