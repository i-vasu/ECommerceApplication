package com.app.shipping;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.order.entities.Shipment;
import com.app.shipping.ShipmentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/shipments")
@SecurityRequirement(name = "E-Commerce Application")
public class ShipmentController implements ShipmentApi {

    @Autowired
    private ShipmentService shipmentService;

    @PostMapping("/create/{orderId}")
    @Override
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
