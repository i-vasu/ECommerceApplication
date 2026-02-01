package com.app.finance.promo.payloads;

import com.app.finance.promo.entities.Coupon;

import java.time.LocalDateTime;
import java.util.Set;

public record CouponDTO(
        Long couponId,
        String code,
        String description,
        Coupon.DiscountType discountType,
        Double discountValue,
        Double minOrderAmount,
        Double maxDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Integer usageLimit,
        Integer usedCount,
        Set<String> applicableCategories,
        boolean active) {
}
