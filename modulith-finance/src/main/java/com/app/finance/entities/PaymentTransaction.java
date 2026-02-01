package com.app.finance.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Enterprise Payment Transaction.
 * Records the granular lifecycle of a payment, mirroring Broadleaf's
 * AUTHORIZE, CAPTURE, SETTLE, REVERSE, REFUND flows.
 */
@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private double amount;

    private String gatewayTransactionId; // e.g., Razerpay Payment ID
    private String responseCode;
    private String responseMessage;

    private LocalDateTime createdAt;

    public enum TransactionType {
        AUTHORIZE,
        CAPTURE,
        SETTLE,
        VOID,
        REFUND,
        REVERSE
    }
}
