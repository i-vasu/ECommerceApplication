package com.app.erp_sync.services;

// import com.app.logistics.shipping.ShipmentService;

import com.app.core.multitenancy.TenantContext;
import com.app.core.multitenancy.TenantManagementService;
import com.app.core.services.RedisLockService;
import com.app.erp_sync.gateway.ERPNextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Log4j2
@RequiredArgsConstructor
@Component
public class SyncScheduler {

    private final ERPNextProductSyncService productSyncService;
    private final ERPNextService erpNextService;
    // private final ShipmentService shipmentService;
    private final RedisLockService redisLockService;
    private final TenantManagementService tenantManagementService;

    // Run every 10 minutes (600,000 ms)
    @Scheduled(fixedRate = 600000)
    public void scheduleProductSync() {
        var tenants = tenantManagementService.getActiveTenants();
        for (var tenant : tenants) {
            TenantContext.runWithTenant(tenant.getTenantId(), () -> {
                if (redisLockService.tryLock("lock:sync:products:" + tenant.getTenantId(),
                        Duration.ofMinutes(5))) {
                    try {
                        log.info("Starting Scheduled Product Sync for Tenant: {}", tenant.getTenantId());
                        productSyncService.syncItems(tenant);
                    } finally {
                        redisLockService.unlock("lock:sync:products:" + tenant.getTenantId());
                    }
                }
            });
        }
    }

    // Run every 5 minutes (300,000 ms)
    @Scheduled(fixedRate = 300000)
    public void scheduleOrderStatusSync() {
        if (redisLockService.tryLock("lock:sync:orders", Duration.ofMinutes(2))) {
            try {
                log.info("Starting Scheduled Order Status Sync from ERPNext...");
                erpNextService.updateOrderStatuses();
            } finally {
                redisLockService.unlock("lock:sync:orders");
            }
        }
    }

    // Shipment status updates are now event-driven via ShipmentStatusUpdatedEvent
    // See ERPEventListener or Logistics module for implementation details.
    /*
     * // Run every 15 minutes (900,000 ms)
     * 
     * @Scheduled(fixedRate = 900000)
     * public void scheduleShipmentSync() {
     * if (redisLockService.tryLock("lock:sync:shipments", Duration.ofMinutes(10)))
     * {
     * try {
     * log.
     * info("Starting Scheduled Shipment Status Sync from Logistics Providers...");
     * shipmentService.updateAllStatuses();
     * } finally {
     * redisLockService.unlock("lock:sync:shipments");
     * }
     * }
     * }
     */
}
