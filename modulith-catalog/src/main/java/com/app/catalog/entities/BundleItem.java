package com.app.catalog.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product Bundle Item.
 * Allows defining a product that is composed of multiple other products.
 * Essential for fashion 'Lookbooks' or 'Summer Sets'.
 */
@Entity
@Table(name = "product_bundle_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BundleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_product_id")
    private Product bundleProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_product_id")
    private Product itemProduct;

    private int quantity;
}
