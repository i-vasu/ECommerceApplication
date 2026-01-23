package com.app.order.listeners;

import com.app.core.events.OrderPaidEvent;
import com.app.shipping.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Log4j2;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class OrderFulfillmentListener {

    private final ShipmentService shipmentService;

    @ApplicationModuleListener
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("Received OrderPaidEvent for Order ID: {}. Triggering automated fulfillment.", event.orderId());
        try {
            // Automatically create shipment and initiate carrier booking
            shipmentService.createShipment(event.orderId());
            log.info("Automated shipment creation successful for Order ID: {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to trigger automated fulfillment for Order ID: {}. Error: {}",
                    event.orderId(), e.getMessage());
            // In a production system, we would publish a 'FulfillmentFailedEvent' or retry
        }
    }
}
