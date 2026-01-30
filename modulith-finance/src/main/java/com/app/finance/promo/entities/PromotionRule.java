package com.app.finance.promo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "promotion_rules")
public class PromotionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String conditionExpression; // SpEL, e.g. "#cart.totalPrice > 5000"

    @Column(length = 1000)
    private String actionExpression; // SpEL, e.g. "#summary.discount(500)"

    private int priority;

    private boolean active = true;

    private String couponCode; // Optional: If this rule requires a specific coupon

    private Long customerSegmentId; // Optional: Restrict to a specific segment

    private boolean combinable = true;
    private boolean exclusive = false;

    public PromotionRule() {
    }

    public PromotionRule(Long id, String name, String conditionExpression, String actionExpression, int priority,
            boolean active, String couponCode, Long customerSegmentId, boolean combinable, boolean exclusive) {
        this.id = id;
        this.name = name;
        this.conditionExpression = conditionExpression;
        this.actionExpression = actionExpression;
        this.priority = priority;
        this.active = active;
        this.couponCode = couponCode;
        this.customerSegmentId = customerSegmentId;
        this.combinable = combinable;
        this.exclusive = exclusive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public String getActionExpression() {
        return actionExpression;
    }

    public void setActionExpression(String actionExpression) {
        this.actionExpression = actionExpression;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public Long getCustomerSegmentId() {
        return customerSegmentId;
    }

    public void setCustomerSegmentId(Long customerSegmentId) {
        this.customerSegmentId = customerSegmentId;
    }

    public boolean isCombinable() {
        return combinable;
    }

    public void setCombinable(boolean combinable) {
        this.combinable = combinable;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }
}
