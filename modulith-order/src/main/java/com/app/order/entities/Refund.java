package com.app.order.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Data
@NoArgsConstructor
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refundId;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String pgRefundId;

    private String pgPaymentId;

    private Double amount;

    private String status;

    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
