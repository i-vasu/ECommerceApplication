package com.app.cart.domain.services;

import com.app.core.APIException;
import com.app.finance.promo.services.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
@RequiredArgsConstructor
public class CartCouponService {

    private static final Logger log = LogManager.getLogger(CartCouponService.class);

    private final CouponService couponService;

    public java.math.BigDecimal applyCouponToCart(String couponCode, java.math.BigDecimal cartTotal) {
        try {
            couponService.validateCoupon(couponCode, cartTotal);
            java.math.BigDecimal discount = couponService.calculateDiscount(couponCode, cartTotal);

            log.info("Applied coupon {} to cart. Discount: {}", couponCode, discount);
            return discount;
        } catch (APIException e) {
            log.warn("Failed to apply coupon {}: {}", couponCode, e.getMessage());
            throw e;
        }
    }

    public java.math.BigDecimal getFinalCartPrice(String couponCode, java.math.BigDecimal cartTotal) {
        if (couponCode == null || couponCode.isEmpty()) {
            return cartTotal;
        }

        java.math.BigDecimal discount = applyCouponToCart(couponCode, cartTotal);
        java.math.BigDecimal finalPrice = cartTotal.subtract(discount);
        return finalPrice.compareTo(java.math.BigDecimal.ZERO) < 0 ? java.math.BigDecimal.ZERO : finalPrice;
    }
}
