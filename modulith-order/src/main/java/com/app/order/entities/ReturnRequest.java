package com.app.order.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_requests")
@Data
@NoArgsConstructor
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long returnRequestId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

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

    public enum ReturnStatus {
        REQUESTED, APPROVED, REJECTED, PICKED_UP, INSPECTED, COMPLETED
    }

    public enum RefundType {
        WALLET, ORIGINAL_SOURCE
    }
}
