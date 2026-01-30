package com.app.finance.promo.services;

import com.app.finance.promo.payloads.CouponDTO;
import java.util.List;

public interface CouponService {

    CouponDTO createCoupon(CouponDTO couponDTO);

    CouponDTO validateCoupon(String code, Double orderAmount);

    Double calculateDiscount(String code, Double orderAmount);

    void applyCoupon(String code, Long userId, Long orderId, Double discountApplied);

    List<CouponDTO> getAllActiveCoupons();

    CouponDTO getCouponByCode(String code);

    void incrementUsageCount(Long couponId);
}
