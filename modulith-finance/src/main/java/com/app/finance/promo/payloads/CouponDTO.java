package com.app.finance.promo.payloads;

import com.app.finance.promo.entities.Coupon;

import java.time.LocalDateTime;
import java.util.Set;

public record CouponDTO(
        Long couponId,
        String code,
        String description,
        Coupon.DiscountType discountType,
        java.math.BigDecimal discountValue,
        java.math.BigDecimal minOrderAmount,
        java.math.BigDecimal maxDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Integer usageLimit,
        Integer usedCount,
        Integer maxUsesPerUser,
        Set<String> applicableCategories,
        boolean active) {
}
