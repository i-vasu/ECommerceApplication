package com.app.order.entities;

import com.app.commerce.states.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    private OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private OrderStatus newStatus;

    private String updatedBy;
    private LocalDateTime updatedAt;
    private String comments;
}
