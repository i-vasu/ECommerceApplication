package com.app.order.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "return_items")
@Data
@NoArgsConstructor
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "return_request_id")
    private ReturnRequest returnRequest;

    @OneToOne
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    private Integer quantity;

    private Double unitRefundAmount; // Pro-rata amount after discounts
}
