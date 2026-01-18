package com.app.inventory;

import com.app.order.OrderCreatedEvent;
import org.jspecify.annotations.NonNull;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryReservationService inventoryService;

    public InventoryEventListener(InventoryReservationService inventoryService) {
        this.inventoryService = inventoryService;
        // TODO:
        // - [ ] Standardize on stable Java 25 / Spring 7 (No Preview)
        // - [ ] Select optimized JDK distribution (Liberica JDK for CRaC)
        // - [x] Implement Unified API Versioning (/api/v1)
        // - [ ] Verify compilation and startup on Java 25 (Delombok workaround)
        // - [x] Implement cross-module communication via Spring Modulith Events
        // - [/] Phase 3: High-Performance Optimization (CRaC, Resilience, Zero-Copy)
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
        } catch (Exception e) {
            log.error("Failed to process inventory update for order: {}", event.orderId(), e);
            // Modulith's event publication registry will handle retries if configured
        }
    }
}
