package com.app.logistics.inventory.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cost_lots")
@Data
@NoArgsConstructor
public class CostLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @CreationTimestamp
    private LocalDateTime purchaseDate;

    @Column(name = "batch_number")
    private String batchNumber;
    
    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    public CostLot(String itemCode, Integer quantity, BigDecimal unitCost, Long warehouseId) {
        this.itemCode = itemCode;
        this.initialQuantity = quantity;
        this.remainingQuantity = quantity;
        this.unitCost = unitCost;
        this.warehouseId = warehouseId;
    }
}
