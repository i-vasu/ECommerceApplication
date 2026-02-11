package com.app.finance.promo.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_usage")
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usageId;

    @ManyToOne
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    private Long userId;

    private Long orderId;

    private java.math.BigDecimal discountApplied;

    @Column(name = "used_at")
    private LocalDateTime usedAt = LocalDateTime.now();

    public CouponUsage() {
    }

    public CouponUsage(Long usageId, Coupon coupon, Long userId, Long orderId, java.math.BigDecimal discountApplied,
            LocalDateTime usedAt) {
        this.usageId = usageId;
        this.coupon = coupon;
        this.userId = userId;
        this.orderId = orderId;
        this.discountApplied = discountApplied;
        this.usedAt = usedAt;
    }

    public Long getUsageId() {
        return usageId;
    }

    public void setUsageId(Long usageId) {
        this.usageId = usageId;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public java.math.BigDecimal getDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(java.math.BigDecimal discountApplied) {
        this.discountApplied = discountApplied;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}
