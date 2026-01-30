package com.app.catalog.entities;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "dynamic_pricing_rules")
@NoArgsConstructor
@AllArgsConstructor
public class DynamicPricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String conditionExpression;

    @Column(columnDefinition = "TEXT")
    private String priceAdjustmentExpression;

    private boolean active = true;

    // Manual Getters and Setters
    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
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

    public String getPriceAdjustmentExpression() {
        return priceAdjustmentExpression;
    }

    public void setPriceAdjustmentExpression(String priceAdjustmentExpression) {
        this.priceAdjustmentExpression = priceAdjustmentExpression;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
