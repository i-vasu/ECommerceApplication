package com.app.logistics.inventory;

import com.app.core.events.OrderCancelledEvent;
import com.app.core.events.OrderCreatedEvent;
import com.app.core.events.ProductCreatedEvent;
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

    public InventoryEventListener(InventoryReservationService inventoryReservationService) {
        this.inventoryReservationService = inventoryReservationService;
    }

    @ApplicationModuleListener
    public void onOrderCreated(@NonNull OrderCreatedEvent event) {
        log.info(">>> Inventory Module: Received OrderCreatedEvent for Order ID: {}", event.orderId());
    }

    @ApplicationModuleListener
    public void onOrderCancelled(@NonNull OrderCancelledEvent event) {
        log.info(">>> Inventory Module: Processing cancellation for Order ID: {}", event.orderId());
        // Releasing stock for all cancelled items in Redis
        for (var item : event.items()) {
            inventoryReservationService.releaseStock(item.itemCode(), item.quantity());
            log.debug("Released stock for item {} due to cancellation", item.itemCode());
        }
    }

    @ApplicationModuleListener
    public void onProductCreated(@NonNull ProductCreatedEvent event) {
        log.info(">>> Inventory Module: Initializing Redis stock for new product: {}", event.itemCode());
        inventoryReservationService.setStock(event.itemCode(), event.quantity());
    }

    @ApplicationModuleListener
    public void onProductUpdated(@NonNull ProductUpdatedEvent event) {
        // Only sync if quantity changed
        if (!event.oldQuantity().equals(event.newQuantity())) {
            log.info(">>> Inventory Module: Updating Redis stock for product: {} (New Qty: {})",
                    event.itemCode(), event.newQuantity());
            inventoryReservationService.setStock(event.itemCode(), event.newQuantity());
        }
    }
}
