package com.app.order.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fulfillment_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private String addressPincode;
    private String addressState;

    private Double shippingCost = 0.0;
    private Double taxAmount = 0.0;

    @OneToMany(mappedBy = "fulfillmentGroup", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();
}
