package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Published when shipment status changes.
 * 
 * Publishers: modulith-logistics (ShipmentService)
 * Listeners: modulith-erp-sync (syncs status to ERP), modulith-order (updates
 * order status)
 * 
 * @param shipmentId     Shipment identifier
 * @param orderId        Associated order ID
 * @param oldStatus      Previous status
 * @param newStatus      Current status
 * @param trackingNumber Carrier tracking number
 * @param carrier        Shipping carrier name
 * @param updatedAt      When status was updated
 */
public record ShipmentStatusUpdatedEvent(
        Long shipmentId,
        Long orderId,
        String customerEmail,
        String oldStatus,
        String newStatus,
        String trackingNumber,
        String carrier,
        LocalDateTime updatedAt) {
    public ShipmentStatusUpdatedEvent(Long shipmentId, Long orderId, String customerEmail, String oldStatus, String newStatus,
            String trackingNumber, String carrier) {
        this(shipmentId, orderId, customerEmail, oldStatus, newStatus, trackingNumber, carrier, LocalDateTime.now());
    }

    public ShipmentStatusUpdatedEvent(Long shipmentId, Long orderId, String oldStatus, String newStatus,
            String trackingNumber, String carrier) {
        this(shipmentId, orderId, null, oldStatus, newStatus, trackingNumber, carrier, LocalDateTime.now());
    }
}
