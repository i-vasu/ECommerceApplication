package com.app.logistics.inventory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryCleanupScheduler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryCleanupScheduler.class);

    private final InventoryReservationService reservationService;

    public InventoryCleanupScheduler(InventoryReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Runs every 5 minutes to reclaim stock from expired reservations (abandoned
     * carts).
     */
    @Scheduled(fixedRate = 300000)
    public void cleanup() {
        log.debug("Inventory Cleanup Job: Scanning for expired reservations...");
        reservationService.releaseExpiredReservations();
    }
}
