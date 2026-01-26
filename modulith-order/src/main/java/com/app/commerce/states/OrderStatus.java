package com.app.commerce.states;

public enum OrderStatus {
    PENDING("PENDING"),
    PAYMENT_CAPTURED("PAYMENT_CAPTURED"),
    PAYMENT_FAILED("PAYMENT_FAILED"),
    PROCESSING("PROCESSING"), // ERP Sync Done
    SHIPPED("SHIPPED"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED"),
    REFUND_INITIATED("REFUND_INITIATED"),
    REFUNDED("REFUNDED");

    private final String status;

    OrderStatus(String status) {
        this.status = status;
    }

    public String getValue() {
        return status;
    }
}
