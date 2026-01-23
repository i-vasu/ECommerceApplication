package com.app.identity.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Data
@NoArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // CREDIT, DEBIT

    private String description;

    private String referenceId; // External order ID or Refund ID

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum TransactionType {
        CREDIT, DEBIT
    }
}
