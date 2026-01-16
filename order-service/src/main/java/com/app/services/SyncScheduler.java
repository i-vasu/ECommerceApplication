package com.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {

    @Autowired
    private ERPNextService erpNextService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private RedisLockService redisLockService;

    // Run every 10 minutes (600,000 ms)
    @Scheduled(fixedRate = 600000)
    public void scheduleProductSync() {
        if (redisLockService.tryLock("lock:sync:products", java.time.Duration.ofMinutes(5))) {
            try {
                System.out.println(">>> Starting Scheduled Product Sync from ERPNext...");
                erpNextService.syncProductsFromERPNext();
            } finally {
                redisLockService.unlock("lock:sync:products");
            }
        } else {
            System.out.println(">>> Product Sync skipped (Process locked by another instance)");
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
