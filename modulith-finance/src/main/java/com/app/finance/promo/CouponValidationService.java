package com.app.finance.promo;

public interface CouponValidationService {
    CouponDiscount validateAndCalculate(String couponCode, java.math.BigDecimal subtotal);

    public record CouponDiscount(java.math.BigDecimal discount, String couponCode) {
    }
}
