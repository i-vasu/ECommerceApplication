package com.app.catalog.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;

    private String itemCode;

    private String color;

    private String size;

    private String material;

    private Integer stockQuantity;

    private boolean allowsBackOrder = false;
    private java.time.LocalDateTime restockDate;

    /**
     * Flag to distinguish between physical variations (Trait) and 
     * value-added variations (Service).
     */
    private boolean isService = false;

    /**
     * Map of GTS properties defining this SKU matrix combo.
     */
    @ManyToMany
    @JoinTable(
        name = "variant_gts_properties",
        joinColumns = @JoinColumn(name = "variant_id"),
        inverseJoinColumns = @JoinColumn(name = "value_id")
    )
    private java.util.List<GtsPropertyValue> gtsProperties = new java.util.ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public java.util.List<GtsPropertyValue> getGtsProperties() {
        return gtsProperties;
    }
}
