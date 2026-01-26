package com.app.order.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shipmentId;

    @OneToOne(mappedBy = "shipment", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Order order;

    private String carrier;
    private String awbNumber;
    private String status;
    private String trackUrl;

    // Shiprocket specific fields
    private String externalOrderId; // Shiprocket order ID
    private String externalShipmentId; // Shiprocket shipment ID
    private String courierName; // Assigned courier name (e.g., "Delhivery", "Blue Dart")

    public Shipment() {
    }

    public Shipment(Long shipmentId, Order order, String carrier, String awbNumber, String status, String trackUrl,
            String externalOrderId, String externalShipmentId, String courierName) {
        this.shipmentId = shipmentId;
        this.order = order;
        this.carrier = carrier;
        this.awbNumber = awbNumber;
        this.status = status;
        this.trackUrl = trackUrl;
        this.externalOrderId = externalOrderId;
        this.externalShipmentId = externalShipmentId;
        this.courierName = courierName;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getAwbNumber() {
        return awbNumber;
    }

    public void setAwbNumber(String awbNumber) {
        this.awbNumber = awbNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrackUrl() {
        return trackUrl;
    }

    public void setTrackUrl(String trackUrl) {
        this.trackUrl = trackUrl;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    public String getExternalShipmentId() {
        return externalShipmentId;
    }

    public void setExternalShipmentId(String externalShipmentId) {
        this.externalShipmentId = externalShipmentId;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }
}
