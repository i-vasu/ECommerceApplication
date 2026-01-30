package com.app.logistics.shipping;

import java.util.Map;
import com.app.logistics.entities.Shipment;
import com.app.core.events.ShipmentRequestedEvent;

public interface ShipmentService {
    Shipment createShipment(ShipmentRequestedEvent event);

    Shipment createShipment(Long orderId);

    Map<String, Object> trackShipment(Long shipmentId);

    void cancelShipment(Long shipmentId);

    void cancelShipmentByOrderId(Long orderId);

    void updateAllStatuses();
}
