package com.app.catalog.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GTS Standard Property (e.g., ID 202 = Fiber Composition, 305 = Zari Type).
 */
@Entity
@Table(name = "catalog_gts_properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtsProperty {

    @Id
    private Long propertyId; // Standard GTS Key ID

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, unique = true)
    private String code;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<GtsPropertyValue> values;
}
