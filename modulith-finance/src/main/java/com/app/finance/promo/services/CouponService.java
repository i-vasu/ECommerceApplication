package com.app.finance.promo.services;

import com.app.finance.promo.payloads.CouponDTO;

import java.util.List;

public interface CouponService {

    CouponDTO createCoupon(CouponDTO couponDTO);

    CouponDTO validateCoupon(String code, java.math.BigDecimal orderAmount);

    java.math.BigDecimal calculateDiscount(String code, java.math.BigDecimal orderAmount);

    void applyCoupon(String code, Long userId, Long orderId, java.math.BigDecimal discountApplied);

    List<CouponDTO> getAllActiveCoupons();

    CouponDTO getCouponByCode(String code);

    void incrementUsageCount(Long couponId);
}
