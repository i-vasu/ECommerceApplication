package com.app.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
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
