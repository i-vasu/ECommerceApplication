package com.app.logistics.shipping;

import com.app.core.events.ShipmentRequestedEvent;
import com.app.logistics.entities.Shipment;

import java.util.Map;

public interface ShipmentService {
    Shipment createShipment(ShipmentRequestedEvent event);

    Shipment createShipment(Long orderId);

    Map<String, Object> trackShipment(Long shipmentId);

    void cancelShipment(Long shipmentId);

    void cancelShipmentByOrderId(Long orderId);

    void updateAllStatuses();
}
