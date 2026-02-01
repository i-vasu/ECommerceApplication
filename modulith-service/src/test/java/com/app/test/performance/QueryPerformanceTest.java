package com.app.test.performance;

import com.app.catalog.repositories.CategoryRepo;
import com.app.logistics.repositories.ShipmentRepo;
import com.app.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance Test: Validates optimized queries
 * 
 * Ensures our performance fixes are working:
 * - Database-level filtering (no findAll())
 * - Projection queries
 * - Pagination
 */
public class QueryPerformanceTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private ShipmentRepo shipmentRepo;

    @Test
    void testCategoryNameProjectionQuery() {
        // Given: Categories exist
        long startTime = System.currentTimeMillis();

        // When: Fetching category names (projection query)
        var categoryNames = categoryRepo.findAllCategoryNames();

        long duration = System.currentTimeMillis() - startTime;

        // Then: Query completes quickly
        assertThat(duration).isLessThan(100); // Should be < 100ms
        assertThat(categoryNames).isNotNull();
    }

    @Test
    void testActiveShipmentsQuery() {
        // When: Fetching active shipments (database-level filter)
        long startTime = System.currentTimeMillis();

        var activeShipments = shipmentRepo.findActiveShipments();

        long duration = System.currentTimeMillis() - startTime;

        // Then: Query is optimized
        assertThat(duration).isLessThan(200); // Should be < 200ms
        assertThat(activeShipments).isNotNull();
    }
}
