package com.app.erp_sync.listeners;

import com.app.core.events.RestockRequestedEvent;
import com.app.core.events.RestockCompletedEvent;
import com.app.core.events.ProductCreatedEvent;
import com.app.core.events.ProductUpdatedEvent;
import com.app.core.events.ProductDeletedEvent;
import com.app.erp_sync.gateway.ERPNextService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listeners for ERP-sync module.
 * Handles product and inventory events by syncing with ERPNext.
 */
@Component
public class ERPNextEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ERPNextEventListener.class);

    private final ERPNextService erpNextService;
    private final ApplicationEventPublisher eventPublisher;

    public ERPNextEventListener(ERPNextService erpNextService, ApplicationEventPublisher eventPublisher) {
        this.erpNextService = erpNextService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handle restock request by triggering purchase order in ERPNext.
     */
    @Async
    @EventListener
    public void handleRestockRequest(RestockRequestedEvent event) {
        log.info("Handling restock request for product: {} ({})", event.productId(), event.itemCode());

        try {
            // Trigger purchase order in ERPNext
            String purchaseOrderId = erpNextService.triggerPurchaseOrder(event.productId(), event.requestedQuantity());

            if (purchaseOrderId != null) {
                log.info("Purchase order {} created in ERPNext for item {}", purchaseOrderId, event.itemCode());

                // Publish completion event
                var completedEvent = new RestockCompletedEvent(
                        event.productId(),
                        event.itemCode(),
                        event.requestedQuantity(),
                        event.requestedQuantity(), // Assuming full quantity received for now
                        purchaseOrderId);
                eventPublisher.publishEvent(completedEvent);
            } else {
                log.warn("Failed to create purchase order in ERPNext for item {}", event.itemCode());
            }
        } catch (Exception e) {
            log.error("Error handling restock request for product {}: {}", event.productId(), e.getMessage(), e);
            // Don't throw - async listeners shouldn't affect main flow
        }
    }

    /**
     * Handle product creation by syncing to ERPNext.
     */
    @Async
    @EventListener
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("Syncing new product to ERPNext: {} ({})", event.productId(), event.itemCode());

        try {
            erpNextService.createOrUpdateItem(event);
            log.info("Product {} successfully synced to ERPNext", event.itemCode());
        } catch (Exception e) {
            log.error("Error syncing product {} to ERPNext: {}", event.productId(), e.getMessage(), e);
        }
    }

    /**
     * Handle product update by syncing to ERPNext.
     */
    @Async
    @EventListener
    public void handleProductUpdated(ProductUpdatedEvent event) {
        log.info("Syncing product update to ERPNext: {} ({})", event.productId(), event.itemCode());

        try {
            erpNextService.createOrUpdateItem(event);
            log.info("Product {} update successfully synced to ERPNext", event.itemCode());
        } catch (Exception e) {
            log.error("Error syncing product update {} to ERPNext: {}", event.productId(), e.getMessage(), e);
        }
    }

    /**
     * Handle product deletion by marking inactive in ERPNext.
     */
    @Async
    @EventListener
    public void handleProductDeleted(ProductDeletedEvent event) {
        log.info("Marking product as inactive in ERPNext: {} ({})", event.productId(), event.itemCode());

        try {
            erpNextService.deactivateItem(event.itemCode());
            log.info("Product {} successfully marked inactive in ERPNext", event.itemCode());
        } catch (Exception e) {
            log.error("Error deactivating product {} in ERPNext: {}", event.productId(), e.getMessage(), e);
        }
    }
}
