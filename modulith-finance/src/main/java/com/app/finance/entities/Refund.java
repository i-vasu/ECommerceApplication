package com.app.finance.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refundId;

    // DECOUPLED: ID reference
    private Long orderId;

    private String pgRefundId;

    private String pgPaymentId;

    private Double amount;

    private String status;

    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Refund() {
    }

    public Refund(Long refundId, Long orderId, String pgRefundId, String pgPaymentId, Double amount, String status,
            String reason, LocalDateTime createdAt) {
        this.refundId = refundId;
        this.orderId = orderId;
        this.pgRefundId = pgRefundId;
        this.pgPaymentId = pgPaymentId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long getRefundId() {
        return refundId;
    }

    public void setRefundId(Long refundId) {
        this.refundId = refundId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPgRefundId() {
        return pgRefundId;
    }

    public void setPgRefundId(String pgRefundId) {
        this.pgRefundId = pgRefundId;
    }

    public String getPgPaymentId() {
        return pgPaymentId;
    }

    public void setPgPaymentId(String pgPaymentId) {
        this.pgPaymentId = pgPaymentId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
