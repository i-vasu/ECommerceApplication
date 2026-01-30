package com.app.core.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when an order is confirmed.
 * Contains necessary data for downstream modules without exposing Order entity.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderConfirmedEvent {
    private Long orderId;
    private String customerEmail;
    private String customerName;
    private Double totalAmount;
    private LocalDateTime confirmedAt;
    private List<OrderItemData> items;
    private String shippingAddress;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemData {
        private String itemCode;
        private String productName;
        private Integer quantity;
        private Double price;
    }
}
