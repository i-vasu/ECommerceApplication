package com.app.intelligence.analysis;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dynamic Pricing Rule powered by SpEL.
 * Defines business logic for adjusting product prices at runtime.
 */
@Entity(name = "IntelligenceDynamicPricingRule")
@Table(name = "intelligence_dynamic_pricing_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntelligenceDynamicPricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String conditionExpression; // e.g., "#product.category.categoryName == 'Electronics' && #user.isVip"

    @Column(length = 1000)
    private String priceAdjustmentExpression; // e.g., "#basePrice * 0.90" (10% discount)

    private int priority;

    private boolean active = true;
}
