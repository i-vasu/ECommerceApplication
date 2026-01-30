package com.app.erp_sync.listeners;

import com.app.core.events.OrderCancelledEvent;
import com.app.core.events.OrderPaidEvent;
import com.app.core.events.ReturnApprovedEvent;
import com.app.erp_sync.gateway.ERPNextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ERPEventListener {

    private final ERPNextService erpNextService;

    @ApplicationModuleListener
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("ERP-Sync: Received OrderCancelledEvent for Order ID: {}. Cancelling ERP Sales Order.",
                event.orderId());
        try {
            erpNextService.cancelSalesOrderByClientOrderId(String.valueOf(event.orderId()));
            log.info("ERP-Sync: ERP Sales Order cancellation requested for Client Order ID: {}", event.orderId());

        } catch (Exception e) {
            log.error("ERP-Sync: Failed to cancel ERP Sales Order for Order ID: {}. Error: {}", event.orderId(),
                    e.getMessage());
        }
    }

    @ApplicationModuleListener
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("ERP-Sync: Received OrderPaidEvent for Order ID: {}. Syncing Sales Order to ERPNext.",
                event.orderId());
        try {
            erpNextService.createSalesOrderByOrderId(event.orderId());
            log.info("ERP-Sync: ERP Sales Order creation requested for Client Order ID: {}", event.orderId());
        } catch (Exception e) {
            log.error("ERP-Sync: Failed to sync ERP Sales Order for Order ID: {}. Error: {}", event.orderId(),
                    e.getMessage());
        }
    }

    @ApplicationModuleListener
    public void onReturnApproved(ReturnApprovedEvent event) {
        log.info("ERP-Sync: Received ReturnApprovedEvent for Order ID: {}. Syncing Return to ERPNext.",
                event.orderId());
        try {
            // TODO: Implement createSalesReturn in ERPNextService
            // erpNextService.createSalesReturn(event);
            log.info("ERP-Sync: ERP Sales Return sync requested for Client Order ID: {}", event.orderId());
        } catch (Exception e) {
            log.error("ERP-Sync: Failed to sync ERP Sales Return for Order ID: {}. Error: {}", event.orderId(),
                    e.getMessage());
        }
    }
}
