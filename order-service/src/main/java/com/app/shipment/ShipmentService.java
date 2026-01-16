package com.app.shipment;

import java.util.Map;

import com.app.entites.Shipment;

public interface ShipmentService {
    Shipment createShipment(Long orderId);

    Map<String, Object> trackShipment(Long shipmentId);

    void cancelShipment(Long shipmentId);

    void updateAllStatuses();
}
