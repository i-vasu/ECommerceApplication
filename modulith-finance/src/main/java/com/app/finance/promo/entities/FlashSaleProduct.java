package com.app.finance.promo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "flash_sale_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "flash_sale_id")
    private FlashSale flashSale;

    private Long productId; // Reference to product

    private java.math.BigDecimal flashPrice;
    private Integer flashQuantity;
    private Integer soldQuantity = 0;

    // To prevent overselling, we'll synchronize this value from Redis
    private Integer reservedQuantity = 0;
}
