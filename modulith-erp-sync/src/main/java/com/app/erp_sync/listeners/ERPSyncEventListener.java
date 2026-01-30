package com.app.erp_sync.listeners;

import com.app.core.events.OrderConfirmedEvent;
import com.app.erp_sync.gateway.ERPNextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for ERP synchronization.
 * Listens to order events and syncs with ERPNext.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class ERPSyncEventListener {

    private final ERPNextService erpNextService;

    @EventListener
    @Async
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Syncing Order #{} to ERPNext", event.getOrderId());
        try {
            // Create sales order in ERPNext using event data
            erpNextService.createSalesOrderByOrderId(event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to sync order {} to ERPNext: {}", event.getOrderId(), e.getMessage());
        }
    }
}
