package com.app.logistics.listeners;

import com.app.core.events.OrderCancelledEvent;
import com.app.core.events.ShipmentRequestedEvent;
import com.app.logistics.shipping.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class LogisticsEventListener {

    private static final Logger log = LoggerFactory.getLogger(LogisticsEventListener.class);
    private final ShipmentService shipmentService;
    private final com.app.core.events.EventIdempotencyService idempotencyService;

    public LogisticsEventListener(ShipmentService shipmentService,
            com.app.core.events.EventIdempotencyService idempotencyService) {
        this.shipmentService = shipmentService;
        this.idempotencyService = idempotencyService;
    }

    @ApplicationModuleListener
    public void onShipmentRequested(ShipmentRequestedEvent event) {
        String eventId = "SHIPMENT-REQ-" + event.orderId();
        if (idempotencyService.isEventProcessed(eventId, "LOGISTICS_MODULE", "ShipmentRequestedEvent")) {
            return;
        }

        log.info("Logistics: Received ShipmentRequestedEvent for Order ID: {}. Initiating shipment creation.",
                event.orderId());
        try {
            shipmentService.createShipment(event);
            log.info("Logistics: Shipment creation successful for Order ID: {}", event.orderId());
            idempotencyService.markEventAsProcessed(eventId, "LOGISTICS_MODULE", "ShipmentRequestedEvent");
        } catch (Exception e) {
            log.error("Logistics: Failed to create shipment for Order ID: {}. Error: {}", event.orderId(),
                    e.getMessage());
        }
    }

    @ApplicationModuleListener
    public void onOrderCancelled(OrderCancelledEvent event) {
        String eventId = "ORDER-CANCEL-" + event.orderId();
        if (idempotencyService.isEventProcessed(eventId, "LOGISTICS_MODULE", "OrderCancelledEvent")) {
            return;
        }

        log.info("Logistics: Received OrderCancelledEvent for Order ID: {}. Checking for shipment cancellation.",
                event.orderId());
        try {
            // Note: Stock restoration is handled by InventoryEventListener in the same module
            shipmentService.cancelShipmentByOrderId(event.orderId());
            log.info("Logistics: Shipment cancellation processed for Order ID: {}",
                    event.orderId());
            idempotencyService.markEventAsProcessed(eventId, "LOGISTICS_MODULE", "OrderCancelledEvent");
        } catch (Exception e) {
            log.error("Logistics: Failed to process order cancellation in logistics for Order ID: {}. Error: {}",
                    event.orderId(),
                    e.getMessage());
        }
    }

    @ApplicationModuleListener
    public void onReturnPickupInitiated(com.app.core.events.ReturnPickupInitiatedEvent event) {
        String eventId = "RETURN-PICKUP-" + event.requestId();
        if (idempotencyService.isEventProcessed(eventId, "LOGISTICS_MODULE", "ReturnPickupInitiatedEvent")) {
            return;
        }

        log.info("Logistics: Received ReturnPickupInitiatedEvent for Request ID: {}. Initiating reverse pickup.",
                event.requestId());
        try {
            shipmentService.initiateReversePickup(event);
            log.info("Logistics: Reverse pickup initiated for Request ID: {}", event.requestId());
            idempotencyService.markEventAsProcessed(eventId, "LOGISTICS_MODULE", "ReturnPickupInitiatedEvent");
        } catch (Exception e) {
            log.error("Logistics: Failed to initiate reverse pickup for Request ID: {}. Error: {}", event.requestId(),
                    e.getMessage());
        }
    }
}
