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

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
