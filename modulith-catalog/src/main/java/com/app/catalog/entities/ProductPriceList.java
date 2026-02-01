package com.app.catalog.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Enterprise Price List Asset.
 * Allows defining specific prices for specific customer segments.
 * Broadleaf logic: A product has a 'Default Price' but can have
 * overrides in different Price Lists.
 */
@Entity
@Table(name = "product_price_lists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private Long customerSegmentId;

    private BigDecimal overridePrice;
    private BigDecimal salePrice;

    private boolean active = true;
}
