package com.app.cart.domain.services;

import com.app.core.APIException;
import com.app.finance.promo.services.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class CartCouponService {

    private final CouponService couponService;

    public Double applyCouponToCart(String couponCode, Double cartTotal) {
        try {
            couponService.validateCoupon(couponCode, cartTotal);
            Double discount = couponService.calculateDiscount(couponCode, cartTotal);

            log.info("Applied coupon {} to cart. Discount: {}", couponCode, discount);
            return discount;
        } catch (APIException e) {
            log.warn("Failed to apply coupon {}: {}", couponCode, e.getMessage());
            throw e;
        }
    }

    public Double getFinalCartPrice(String couponCode, Double cartTotal) {
        if (couponCode == null || couponCode.isEmpty()) {
            return cartTotal;
        }

        Double discount = applyCouponToCart(couponCode, cartTotal);
        return Math.max(0, cartTotal - discount);
    }
}
