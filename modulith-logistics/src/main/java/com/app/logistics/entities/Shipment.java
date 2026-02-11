package com.app.logistics.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shipmentId;

    private Long orderId;

    private String carrier;
    private String awbNumber;
    private String status;
    private String trackUrl;

    // Shiprocket specific fields
    private String externalOrderId; // Shiprocket order ID
    private String externalShipmentId; // Shiprocket shipment ID
    private String courierName; // Assigned courier name (e.g., "Delhivery", "Blue Dart")
    
    private String manifestUrl; // Generated Manifest PDF URL

    public Shipment() {
    }

    public Shipment(Long shipmentId, Long orderId, String carrier, String awbNumber, String status, String trackUrl,
            String externalOrderId, String externalShipmentId, String courierName) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public String getManifestUrl() {
        return manifestUrl;
    }

    public void setManifestUrl(String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }
}
