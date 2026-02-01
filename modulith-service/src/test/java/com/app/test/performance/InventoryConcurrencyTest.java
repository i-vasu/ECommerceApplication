package com.app.test.performance;

import com.app.logistics.inventory.InventoryService;
import com.app.logistics.inventory.InventoryService.InventoryLock;
import com.app.logistics.inventory.payloads.InventoryRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Granular Concurrency Test.
 * Verifies that the Inventory system is atomic and handles 'Thundering Herd'
 * scenarios.
 */
@SpringBootTest
public class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    @DisplayName("🚀 Concurrency: Ensure no overselling during 100 parallel checkout requests")
    void testAtomicStockDeduction() throws InterruptedException {
        String itemCode = "LIMITED-EDITION-WATCH";
        int initialStock = 10;
        int threads = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    List<InventoryRequest> requests = List.of(new InventoryRequest(itemCode, 1));
                    InventoryLock result = inventoryService.lockStock(requests);

                    if (result.locked()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Log error
                }
            });
        }

        latch.countDown(); // Release all threads at once
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verify: We had 10 items. Success count must be exactly 10.
        // No overselling!
        assertEquals(10, successCount.get(), "Overselling detected! More than 10 locks were granted.");
        assertEquals(90, failureCount.get(), "Failure count mismatched.");

        System.out.println(
                "✅ Inventory Atomicity verified: Success=" + successCount.get() + ", Refusals=" + failureCount.get());
    }
}
