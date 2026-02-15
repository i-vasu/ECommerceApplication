package com.app.logistics.shipping;

import com.app.logistics.entities.Shipment;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/shipments")
@SecurityRequirement(name = "E-Commerce Application")
public class ShipmentController implements ShipmentApi {

    @Autowired
    private ShipmentService shipmentService;

    @PostMapping("/create/{orderId}")
    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('WAREHOUSE_MANAGER')")
    public ResponseEntity<Shipment> createShipment(@PathVariable Long orderId) {
        Shipment shipment = shipmentService.createShipment(orderId);
        return new ResponseEntity<Shipment>(shipment, HttpStatus.CREATED);
    }

    @GetMapping("/track/{shipmentId}")
    @Override
    public ResponseEntity<Map<String, Object>> trackShipment(@PathVariable Long shipmentId) {
        Map<String, Object> trackingData = shipmentService.trackShipment(shipmentId);
        return new ResponseEntity<Map<String, Object>>(trackingData, HttpStatus.OK);
    }

}
