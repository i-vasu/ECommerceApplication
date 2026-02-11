package com.app.logistics.shipping;

import com.app.logistics.entities.Shipment;
import com.app.logistics.shipping.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/logistics")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class LogisticsController {

    private final ShipmentService shipmentService;

    public LogisticsController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping("/pick/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Shipment> pickOrder(@PathVariable Long orderId) {
        Shipment shipment = shipmentService.markAsPicked(orderId);
        return ResponseEntity.ok(shipment);
    }

    @PostMapping("/pack/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Shipment> packOrder(@PathVariable Long orderId) {
        Shipment shipment = shipmentService.markAsPacked(orderId);
        return ResponseEntity.ok(shipment);
    }

    @PostMapping("/manifest/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createManifest(@PathVariable Long orderId) {
        String url = shipmentService.generateManifest(orderId);
        return ResponseEntity.ok(Map.of("manifestUrl", url));
    }

    @GetMapping("/track/{shipmentId}")
    public ResponseEntity<Map<String, Object>> track(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(shipmentService.trackShipment(shipmentId));
    }
}
