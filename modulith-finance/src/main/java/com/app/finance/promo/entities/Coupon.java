package com.app.finance.promo.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(unique = true, nullable = false)
    private String code;

    private String description;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private Double discountValue;

    private Double minOrderAmount;

    private Double maxDiscountAmount;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private Integer usageLimit;

    private Integer usedCount = 0;

    @ElementCollection
    @CollectionTable(name = "coupon_categories", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "category_name")
    private Set<String> applicableCategories;

    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    public Coupon() {
    }

    public Coupon(Long couponId, String code, String description, DiscountType discountType, Double discountValue,
            Double minOrderAmount, Double maxDiscountAmount, LocalDateTime validFrom, LocalDateTime validTo,
            Integer usageLimit, Integer usedCount, Set<String> applicableCategories, boolean active,
            LocalDateTime createdAt) {
        this.couponId = couponId;
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
        this.applicableCategories = applicableCategories;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public Double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
    }

    public Double getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(Double minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public Double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(Double maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public Set<String> getApplicableCategories() {
        return applicableCategories;
    }

    public void setApplicableCategories(Set<String> applicableCategories) {
        this.applicableCategories = applicableCategories;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
