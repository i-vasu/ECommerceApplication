package com.app.commerce.promotion.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotion_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
