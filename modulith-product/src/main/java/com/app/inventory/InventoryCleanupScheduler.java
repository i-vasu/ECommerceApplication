package com.app.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class InventoryCleanupScheduler {

    private final InventoryReservationService reservationService;

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
