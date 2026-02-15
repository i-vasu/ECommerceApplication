package com.app.logistics.inventory.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_moves")
@Data
@NoArgsConstructor
public class StockMove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "from_warehouse_id", nullable = false)
    private Long fromWarehouseId;

    @Column(name = "from_bin_id")
    private Long fromBinId;

    @Column(name = "to_warehouse_id", nullable = false)
    private Long toWarehouseId;

    @Column(name = "to_bin_id")
    private Long toBinId;

    @Column(name = "reference_id")
    private String referenceId; // Transfer Order #, etc.

    @Column(name = "reason")
    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "executed_by")
    private String executedBy;
}
