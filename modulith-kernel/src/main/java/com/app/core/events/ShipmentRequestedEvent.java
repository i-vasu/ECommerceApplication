package com.app.core.events;

import java.math.BigDecimal;
import java.util.List;

public record ShipmentRequestedEvent(
    Long orderId,
    String email,
    ShippingAddress address,
    List<ShipmentItem> items,
    BigDecimal totalValue,
    boolean isCod
) {
    public record ShippingAddress(
        String name, String phone, String street, String city, String state, String country, String pincode
    ) {}

    public record ShipmentItem(
        String productName, String sku, int quantity, BigDecimal price, double weight,
        double length, double width, double height
    ) {}
}
