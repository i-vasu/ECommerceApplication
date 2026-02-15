package com.app.logistics.inventory.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "inventory")
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(InventoryId.class)
public class Inventory {

    @Id
    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Id
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Id
    @Column(name = "bin_id", nullable = false)
    private Long binId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Version
    private Long version;
}
