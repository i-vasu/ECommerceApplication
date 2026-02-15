package com.app.catalog.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Specific value for an attribute (e.g., "Pure Gold", "Temple Border").
 */
@Entity
@Table(name = "catalog_attribute_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long valueId;

    @Column(nullable = false)
    private String value;

    @Column(nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id")
    private Attribute attribute;
}
