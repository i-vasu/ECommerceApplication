package com.app.commerce.promotion;

import com.app.discount.services.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@org.springframework.context.annotation.Primary
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final CouponService couponService;

    @Override
    public BigDecimal applyCoupon(String code, BigDecimal subTotal, String email) {
        try {
            // Validate first
            couponService.validateCoupon(code, subTotal.doubleValue());

            // Calculate discount
            Double discount = couponService.calculateDiscount(code, subTotal.doubleValue());

            // Return as negative BigDecimal
            return BigDecimal.valueOf(discount).negate();
        } catch (Exception e) {
            // Silently return ZERO for pricing module if invalid
            return BigDecimal.ZERO;
        }
    }
}
