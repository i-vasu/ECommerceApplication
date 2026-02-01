package com.app.logistics.shipping;

import com.app.core.ResourceNotFoundException;
import com.app.core.async.EventProducer;
import com.app.core.events.ShipmentRequestedEvent;
import com.app.core.events.ShipmentStatusUpdatedEvent;
import com.app.logistics.entities.Shipment;
import com.app.logistics.external.ShadowfaxClient;
import com.app.logistics.repositories.ShipmentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ShipmentServiceImpl implements ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentServiceImpl.class);

    public enum ShippingProvider {
        SHIPROCKET, SHADOWFAX
    }

    @Autowired
    private ShiprocketService shiprocketService;

    @Autowired(required = false)
    private ShadowfaxClient shadowfaxClient;

    @Autowired
    private com.app.core.multitenancy.TenantManagementService tenantManagementService;

    @Autowired
    private ShipmentRepo shipmentRepo;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private com.app.governance.states.OperationalStateMachineService stateMachineService;

    @Override
    @Transactional
    public Shipment createShipment(ShipmentRequestedEvent event) {
        Shipment existing = shipmentRepo.findByOrderId(event.orderId());
        if (existing != null) {
            log.info("Shipment already exists for order {}", event.orderId());
            return existing;
        }

        ShippingProvider provider = ShippingProvider.SHIPROCKET;
        Shipment shipment;
        switch (provider) {
            case SHIPROCKET:
                shipment = createShiprocketShipment(event);
                break;
            case SHADOWFAX:
                throw new RuntimeException("Shadowfax provider pending refactor");
            default:
                throw new RuntimeException("Unsupported shipping provider: " + provider);
        }

        shipment.setOrderId(event.orderId());
        return shipmentRepo.save(shipment);
    }

    @Override
    @Transactional
    public Shipment createShipment(Long orderId) {
        throw new UnsupportedOperationException(
                "Shipment creation requires full ShipmentRequestedEvent for decoupling.");
    }

    private Shipment createShiprocketShipment(ShipmentRequestedEvent event) {
        log.info("Creating Shiprocket shipment for order {}", event.orderId());
        Map<String, Object> response = shiprocketService.createShipment(event);

        Shipment shipment = new Shipment();
        shipment.setOrderId(event.orderId());
        shipment.setCarrier("Shiprocket");

        if (response.containsKey("order_id")) {
            shipment.setExternalOrderId(String.valueOf(response.get("order_id")));
        }
        if (response.containsKey("shipment_id")) {
            shipment.setExternalShipmentId(String.valueOf(response.get("shipment_id")));
        }
        if (response.containsKey("awb_code")) {
            shipment.setAwbNumber(String.valueOf(response.get("awb_code")));
        }

        shipment.setStatus("CREATED");
        shipment = shipmentRepo.save(shipment);

        stateMachineService.triggerShipmentEvent(shipment.getShipmentId(),
                com.app.governance.states.ShipmentEvent.ASSIGN_CARRIER);

        return shipment;
    }

    @Override
    public Map<String, Object> trackShipment(Long shipmentId) {
        Shipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        String providerStr = tenantManagementService.getCurrentTenant().getShippingProvider();
        if (providerStr == null || providerStr.isEmpty()) {
            providerStr = "SHIPROCKET";
        }
        ShippingProvider provider = ShippingProvider.valueOf(providerStr.toUpperCase());

        switch (provider) {
            case SHIPROCKET:
                if (shipment.getAwbNumber() != null) {
                    return shiprocketService.trackByAwb(shipment.getAwbNumber());
                } else if (shipment.getExternalShipmentId() != null) {
                    return shiprocketService.trackByShipmentId(shipment.getExternalShipmentId());
                }
                throw new RuntimeException("No AWB or shipment ID available for tracking");
            case SHADOWFAX:
                if (shadowfaxClient == null)
                    throw new RuntimeException("Shadowfax client not configured");
                String token = tenantManagementService.getCurrentTenant().getShadowfaxToken();
                return shadowfaxClient.trackOrder(token, shipment.getAwbNumber());
            default:
                throw new RuntimeException("Unsupported shipping provider: " + providerStr);
        }
    }

    public Shipment generateAwb(Long shipmentId, String courierId) {
        Shipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        if (shipment.getAwbNumber() != null && !shipment.getAwbNumber().isEmpty()) {
            return shipment;
        }

        Map<String, Object> response = shiprocketService.generateAwb(
                shipment.getExternalShipmentId(), courierId);

        if (response.containsKey("awb_code")) {
            shipment.setAwbNumber(String.valueOf(response.get("awb_code")));
            shipment.setStatus("AWB_ASSIGNED");
            return shipmentRepo.save(shipment);
        }
        throw new RuntimeException("Failed to generate AWB: " + response);
    }

    public Shipment requestPickup(Long shipmentId) {
        Shipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        shiprocketService.requestPickup(shipment.getExternalShipmentId());
        shipment.setStatus("PICKUP_REQUESTED");

        stateMachineService.triggerShipmentEvent(shipmentId, com.app.governance.states.ShipmentEvent.PICKUP);
        return shipmentRepo.save(shipment);
    }

    @Override
    @Transactional
    public void cancelShipment(Long shipmentId) {
        Shipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        if ("Shiprocket".equals(shipment.getCarrier()) && shipment.getExternalOrderId() != null) {
            shiprocketService.cancelOrder(List.of(shipment.getExternalOrderId()));
        }

        shipment.setStatus("CANCELLED");
        shipmentRepo.save(shipment);
    }

    @Override
    @Transactional
    public void cancelShipmentByOrderId(Long orderId) {
        Shipment shipment = shipmentRepo.findByOrderId(orderId);
        if (shipment != null) {
            cancelShipment(shipment.getShipmentId());
        }
    }

    @Override
    public void updateAllStatuses() {
        List<Shipment> activeShipments = shipmentRepo.findActiveShipments();
        for (Shipment shipment : activeShipments) {
            try {
                if ("Shiprocket".equals(shipment.getCarrier())) {
                    Map<String, Object> tracking;
                    if (shipment.getAwbNumber() != null) {
                        tracking = shiprocketService.trackByAwb(shipment.getAwbNumber());
                    } else {
                        tracking = shiprocketService.trackByShipmentId(shipment.getExternalShipmentId());
                    }

                    if (tracking.containsKey("tracking_data")) {
                        Map<String, Object> tData = castToMap(tracking.get("tracking_data"));
                        String status = (String) tData.get("track_status");
                        if (status != null && !status.equalsIgnoreCase(shipment.getStatus())) {
                            shipment.setStatus(status);
                            if (status.equalsIgnoreCase("DELIVERED")) {
                                stateMachineService.triggerShipmentEvent(shipment.getShipmentId(),
                                        com.app.governance.states.ShipmentEvent.ARRIVE_AT_DESTINATION);
                            } else if (status.equalsIgnoreCase("SHIPPED")) {
                                stateMachineService.triggerShipmentEvent(shipment.getShipmentId(),
                                        com.app.governance.states.ShipmentEvent.SHIP);
                            }
                            shipmentRepo.save(shipment);
                            try {
                                ShipmentStatusUpdatedEvent event = new ShipmentStatusUpdatedEvent(
                                        shipment.getShipmentId(),
                                        shipment.getOrderId(),
                                        shipment.getStatus(),
                                        status,
                                        shipment.getAwbNumber(),
                                        shipment.getCarrier());
                                eventProducer.publish("shipment_status_updates", event);
                            } catch (Exception px) {
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private Map<String, Object> castToMap(Object obj) {
        if (obj instanceof Map)
            return (Map<String, Object>) obj;
        return Collections.emptyMap();
    }
}
