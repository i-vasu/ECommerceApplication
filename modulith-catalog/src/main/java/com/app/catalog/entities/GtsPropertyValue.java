package com.app.catalog.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GTS Standard Property Value (e.g., ID 50 = Pure Silk).
 */
@Entity
@Table(name = "catalog_gts_property_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtsPropertyValue {

    @Id
    private Long valueId; // Standard GTS Value ID

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private GtsProperty property;

    public String getCode() {
        return code;
    }

    public GtsProperty getProperty() {
        return property;
    }
}
