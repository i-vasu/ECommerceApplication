package com.app.marketplace.dto.external;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

public class ExternalOrderDTO {
    // Matching com.app.payloads.OrderDTO in order-service
    private Long orderId;
    private String email;
    // We'll use a simplified item structure or a dedicated one
    private List<ExternalOrderItemDTO> orderItems;
    private BigDecimal totalAmount;
    private String orderStatus;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<ExternalOrderItemDTO> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<ExternalOrderItemDTO> orderItems) {
        this.orderItems = orderItems;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
