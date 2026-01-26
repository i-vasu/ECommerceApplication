package com.app.core.events;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderStatusEvent implements Serializable {
    private Long orderId;
    private String userEmail;
    private String status; // SHIPPED, DELIVERED, CANCELLED
    private String trackingNumber;
    private String carrier;
    private String tenantId;
    private LocalDateTime timestamp;

    public OrderStatusEvent() {
    }

    public OrderStatusEvent(Long orderId, String userEmail, String status, String trackingNumber, String carrier,
            String tenantId) {
        this.orderId = orderId;
        this.userEmail = userEmail;
        this.status = status;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.tenantId = tenantId;
        this.timestamp = LocalDateTime.now();
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
