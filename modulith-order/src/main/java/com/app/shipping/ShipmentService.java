package com.app.shipping;

import java.util.Map;

import com.app.order.entities.Shipment;

public interface ShipmentService {
    Shipment createShipment(Long orderId);

    Map<String, Object> trackShipment(Long shipmentId);

    void cancelShipment(Long shipmentId);

    void updateAllStatuses();
}
