package com.app.logistics.inventory.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.envers.Audited;

@Entity
@Table(name = "bins")
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bin_code", nullable = false)
    private String binCode; // e.g., A-01-12 (Aisle-Shelf-Position)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    private String zone; // e.g., Cold Storage, Returns, Fast Moving
    
    private boolean active = true;
}
