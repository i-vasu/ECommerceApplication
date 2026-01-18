package com.app.discount;

public interface CouponValidationService {
    CouponDiscount validateAndCalculate(String couponCode, double subtotal);

    public record CouponDiscount(double discount, String couponCode) {
    }
}
