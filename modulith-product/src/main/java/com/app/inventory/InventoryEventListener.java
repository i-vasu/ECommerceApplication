package com.app.inventory;

import com.app.core.events.OrderCreatedEvent;
import org.jspecify.annotations.NonNull;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryEventListener.class);

    public InventoryEventListener() {
    }

    @ApplicationModuleListener
    public void onOrderCreated(@NonNull OrderCreatedEvent event) {
        log.info(">>> Inventory Module: Received OrderCreatedEvent for Order ID: {}", event.orderId());

        // In a real scenario, we would finalize the stock deduction here
        // The OrderService already reserved the stock in Redis,
        // so here we might confirm the deduction in the primary DB or ERPNext.
        try {
            log.info(">>> Finalizing stock deduction for order {}...", event.orderId());
            // Logic to sync with ERPNext or primary DB would go here
            // For now, we just log the event
        } catch (Exception e) {
            log.error("Failed to process inventory update for order: {}", event.orderId(), e);
            // Modulith's event publication registry will handle retries if configured
        }
    }
}
