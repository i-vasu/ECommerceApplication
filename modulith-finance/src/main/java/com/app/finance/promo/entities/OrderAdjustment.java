package com.app.finance.promo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_adjustments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DECOUPLED: Pointer to Order ID instead of JPA association
    private Long orderId;

    private String promotionName;
    private String couponCode;
    private double adjustmentValue; // e.g., -500.0
    private String reason; // e.g., "Seasonal Sale", "Customer Reward"
}
