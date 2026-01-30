package com.app.order.services;

import com.app.core.events.OrderStatusEvent;
import com.app.governance.states.OperationalStateMachineService;
import com.app.governance.states.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/**
 * Choreography Orchestrator for Cross-Module State Synchronization.
 * Synchronizes Order lifecycle with Shipment, Payment, and Identity events.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class CrossModuleOrchestrationService {

    private final OperationalStateMachineService stateMachineService;

    @ApplicationModuleListener
    public void onShipmentStatusUpdate(OrderStatusEvent event) {
        log.info("Cross-Module Event: Shipment status for Order {} updated to {}", event.orderId(), event.status());

        // Synchronization Logic: If Shipment is DELIVERED, Trigger Order DELIVER
        if ("DELIVERED".equalsIgnoreCase(event.status())) {
            log.info("Synchronizing Order {} to DELIVERED state via Choreography.", event.orderId());
            stateMachineService.triggerOrderEvent(event.orderId(), OrderEvent.DELIVER);
        } else if ("SHIPPED".equalsIgnoreCase(event.status())) {
            log.info("Synchronizing Order {} to SHIPPED state via Choreography.", event.orderId());
            stateMachineService.triggerOrderEvent(event.orderId(), OrderEvent.SHIP);
        }
    }
}
