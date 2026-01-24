package com.app.marketplace.dto.external;

import lombok.Data;
import java.math.BigDecimal;

public class ExternalOrderItemDTO {
    // Matching com.app.payloads.OrderItemDTO in order-service
    private Long orderItemId;
    // We might need product info
    private Long productId;
    private Integer quantity;
    private Double discount;
    private Double orderedProductPrice;

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getOrderedProductPrice() {
        return orderedProductPrice;
    }

    public void setOrderedProductPrice(Double orderedProductPrice) {
        this.orderedProductPrice = orderedProductPrice;
    }
}
