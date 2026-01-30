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

    public LogisticsEventListener(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @ApplicationModuleListener
    public void onShipmentRequested(ShipmentRequestedEvent event) {
        log.info("Logistics: Received ShipmentRequestedEvent for Order ID: {}. Initiating shipment creation.",
                event.orderId());
        try {
            shipmentService.createShipment(event);
            log.info("Logistics: Shipment creation successful for Order ID: {}", event.orderId());
        } catch (Exception e) {
            log.error("Logistics: Failed to create shipment for Order ID: {}. Error: {}", event.orderId(),
                    e.getMessage());
        }
    }

    @ApplicationModuleListener
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Logistics: Received OrderCancelledEvent for Order ID: {}. Checking for shipment cancellation.",
                event.orderId());
        try {
            shipmentService.cancelShipmentByOrderId(event.orderId());
            log.info("Logistics: Shipment cancellation processed for Order ID: {}", event.orderId());
        } catch (Exception e) {
            log.error("Logistics: Failed to process shipment cancellation for Order ID: {}. Error: {}", event.orderId(),
                    e.getMessage());
        }
    }
}
