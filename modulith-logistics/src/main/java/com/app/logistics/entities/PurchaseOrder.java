package com.app.logistics.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Data
@NoArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String supplierName;

    private String supplierEmail;

    @Enumerated(EnumType.STRING)
    private POStatus status; // DRAFT, SENT, RECEIVED, CANCELLED

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    private Double totalAmount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime receivedAt;

    public enum POStatus {
        DRAFT, SENT, RECEIVED, CANCELLED
    }
}
