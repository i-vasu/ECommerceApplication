package com.app.support.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long returnRequestId;

    @Column(name = "order_id")
    private Long orderId;

    private String userEmail;

    private String reason;

    @Enumerated(EnumType.STRING)
    private ReturnStatus status; // REQUESTED, APPROVED, REJECTED, PICKED_UP, INSPECTED, COMPLETED

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items = new ArrayList<>();

    private Double refundAmount;

    private String adminComments;

    @Enumerated(EnumType.STRING)
    private RefundType refundType; // WALLET, ORIGINAL_SOURCE

    @CreationTimestamp
    private LocalDateTime createdAt;

    public ReturnRequest() {}

    public Long getReturnRequestId() { return returnRequestId; }
    public void setReturnRequestId(Long returnRequestId) { this.returnRequestId = returnRequestId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public ReturnStatus getStatus() { return status; }
    public void setStatus(ReturnStatus status) { this.status = status; }

    public List<ReturnItem> getItems() { return items; }
    public void setItems(List<ReturnItem> items) { this.items = items; }

    public Double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Double refundAmount) { this.refundAmount = refundAmount; }

    public String getAdminComments() { return adminComments; }
    public void setAdminComments(String adminComments) { this.adminComments = adminComments; }

    public RefundType getRefundType() { return refundType; }
    public void setRefundType(RefundType refundType) { this.refundType = refundType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum ReturnStatus {
        REQUESTED, APPROVED, REJECTED, PICKED_UP, INSPECTED, COMPLETED
    }

    public enum RefundType {
        WALLET, ORIGINAL_SOURCE
    }
}
