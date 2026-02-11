package com.app.logistics.inventory;

import com.app.core.events.OrderCancelledEvent;
import com.app.core.events.OrderCreatedEvent;
import com.app.core.events.ReturnApprovedEvent;
import com.app.core.events.ProductCreatedEvent;
import com.app.core.events.ProductDeletedEvent;
import com.app.core.events.ProductUpdatedEvent;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryReservationService inventoryReservationService;

    private final com.app.core.events.EventIdempotencyService idempotencyService;

    public InventoryEventListener(InventoryReservationService inventoryReservationService,
                                  com.app.core.events.EventIdempotencyService idempotencyService) {
        this.inventoryReservationService = inventoryReservationService;
        this.idempotencyService = idempotencyService;
    }

    @ApplicationModuleListener
    public void onOrderCreated(@NonNull OrderCreatedEvent event) {
        log.info(">>> Inventory Module: Received OrderCreatedEvent for Order ID: {}", event.orderId());
    }

    @ApplicationModuleListener
    public void onOrderCancelled(@NonNull OrderCancelledEvent event) {
        String eventId = "ORDER-CANCEL-" + event.orderId();
        if (idempotencyService.isEventProcessed(eventId, "INVENTORY_MODULE", "OrderCancelledEvent")) {
            return;
        }

        log.info(">>> Inventory Module: Processing cancellation for Order ID: {}", event.orderId());
        // Releasing stock for all cancelled items in Redis
        for (var item : event.items()) {
            inventoryReservationService.releaseStock(item.itemCode(), item.quantity());
            log.debug("Released stock for item {} due to cancellation", item.itemCode());
        }

        idempotencyService.markEventAsProcessed(eventId, "INVENTORY_MODULE", "OrderCancelledEvent");
    }

    @ApplicationModuleListener
    public void onProductCreated(@NonNull ProductCreatedEvent event) {
        log.info(">>> Inventory Module: Initializing inventory for new product: {}", event.itemCode());
        
        // 1. Initialize DB & Redis
        inventoryReservationService.initializeInventory(event.itemCode(), event.quantity());
    }

    @ApplicationModuleListener
    public void onProductUpdated(@NonNull ProductUpdatedEvent event) {
        // Only sync if quantity changed
        if (!event.oldQuantity().equals(event.newQuantity())) {
            log.info(">>> Inventory Module: Syncing DB & Redis stock for product: {} (New Qty: {})",
                    event.itemCode(), event.newQuantity());
            inventoryReservationService.updateInventoryStock(event.itemCode(), event.newQuantity());
        }
    }

    @ApplicationModuleListener
    public void onProductDeleted(@NonNull ProductDeletedEvent event) {
        log.info(">>> Inventory Module: Product deleted: {}. Keeping inventory record for audit trail.", 
                event.itemCode());
        // In a real ERP, we might mark inventory as 'Inactive' or 'Discontinued'
    }

    @ApplicationModuleListener
    public void onReturnApproved(@NonNull ReturnApprovedEvent event) {
        log.info(">>> Inventory Module: Processing return approval for order {}", event.orderId());
        
        for (ReturnApprovedEvent.ApprovedReturnItem item : event.items()) {
            inventoryReservationService.restock(item.itemCode(), item.quantity(), "Customer Return Approved: Order #" + event.orderId());
        }
    }
}
