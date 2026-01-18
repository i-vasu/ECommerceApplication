package com.app.admin.services;

import com.app.shipping.ShipmentService;
import com.app.order.services.ERPNextService;
import com.app.core.services.RedisLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {

    @Autowired
    private com.app.admin.services.ERPNextProductSyncService productSyncService;

    @Autowired
    private ERPNextService erpNextService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private RedisLockService redisLockService;

    @Autowired
    private com.app.core.multitenancy.TenantManagementService tenantManagementService;

    @Autowired
    private com.app.core.multitenancy.TenantRepository tenantRepository;

    // Run every 10 minutes (600,000 ms)
    @Scheduled(fixedRate = 600000)
    public void scheduleProductSync() {
        var tenants = tenantManagementService.getActiveTenants();
        for (var tenant : tenants) {
            com.app.core.multitenancy.TenantContext.setTenantId(tenant.getTenantId());
            try {
                if (redisLockService.tryLock("lock:sync:products:" + tenant.getTenantId(),
                        java.time.Duration.ofMinutes(5))) {
                    try {
                        System.out.println(">>> Starting Scheduled Product Sync for Tenant: " + tenant.getTenantId());
                        productSyncService.syncItems(tenant);
                    } finally {
                        redisLockService.unlock("lock:sync:products:" + tenant.getTenantId());
                    }
                }
            } finally {
                com.app.core.multitenancy.TenantContext.clear();
            }
        }
    }

    // Run every 5 minutes (300,000 ms)
    @Scheduled(fixedRate = 300000)
    public void scheduleOrderStatusSync() {
        if (redisLockService.tryLock("lock:sync:orders", java.time.Duration.ofMinutes(2))) {
            try {
                System.out.println(">>> Starting Scheduled Order Status Sync from ERPNext...");
                erpNextService.updateOrderStatuses();
            } finally {
                redisLockService.unlock("lock:sync:orders");
            }
        } else {
            // System.out.println("Locked");
        }
    }

    // Run every 15 minutes (900,000 ms)
    @Scheduled(fixedRate = 900000)
    public void scheduleShipmentSync() {
        if (redisLockService.tryLock("lock:sync:shipments", java.time.Duration.ofMinutes(10))) {
            try {
                System.out.println(">>> Starting Scheduled Shipment Status Sync from Logistics Providers...");
                shipmentService.updateAllStatuses();
            } finally {
                redisLockService.unlock("lock:sync:shipments");
            }
        } else {
            // System.out.println("Locked");
        }
    }
}
