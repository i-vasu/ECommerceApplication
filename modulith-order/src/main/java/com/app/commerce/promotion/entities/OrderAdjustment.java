package com.app.commerce.promotion.entities;

import com.app.order.entities.Order;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private String promotionName;
    private String couponCode;
    private double adjustmentValue; // e.g., -500.0
    private String reason; // e.g., "Seasonal Sale", "Customer Reward"
}
