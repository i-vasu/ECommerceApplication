package com.app.shipping;

import com.app.order.entites.Shipment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Tag(name = "Shipment", description = "Shipment and Tracking APIs")
public interface ShipmentApi {

    @Operation(summary = "Create Shipment", description = "Creates a shipment for an order")
    ResponseEntity<Shipment> createShipment(Long orderId);

    @Operation(summary = "Track Shipment", description = "Retrieves tracking information for a shipment")
    ResponseEntity<Map<String, Object>> trackShipment(Long shipmentId);
}
