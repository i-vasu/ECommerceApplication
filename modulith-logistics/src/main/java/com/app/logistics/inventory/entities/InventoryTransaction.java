package com.app.logistics.inventory.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Data
@NoArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "reference_id")
    private String referenceId; // Order ID, PO Number, etc.

    @Column(name = "reason")
    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum TransactionType {
        INBOUND_PO,      // Purchase Order Receipt
        OUTBOUND_ORDER,  // Sales Order Fulfillment
        RETURN_RESTOCK,  // Customer Return
        ADJUSTMENT,      // Manual Correction
        MANUAL_ADJUSTMENT, // Custom adjustment from Admin
        RESERVATION,     // Temporary Lock
        RELEASE          // Reservation Release
    }
}
