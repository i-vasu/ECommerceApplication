package com.app.finance.promo;

import com.app.finance.promo.services.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final CouponService couponService;

    @Override
    public BigDecimal applyCoupon(String code, BigDecimal subTotal, String email) {
        try {
            // Validate first
            couponService.validateCoupon(code, subTotal);

            // Calculate discount
            java.math.BigDecimal discount = couponService.calculateDiscount(code, subTotal);

            // Return as negative BigDecimal
            return discount.negate();
        } catch (Exception e) {
            // Silently return ZERO for pricing module if invalid
            return BigDecimal.ZERO;
        }
    }
}
