package com.app.tests.utils;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton TestContainer manager for database integration tests
 */
public class TestContainersManager {

    private static PostgreSQLContainer<?> postgresContainer;

    /**
     * Get or create a shared PostgreSQL container
     * Reuses the same container across all tests for performance
     */
    public static PostgreSQLContainer<?> getPostgresContainer() {
        if (postgresContainer == null) {
            postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true); // Reuse container between test runs
        }

        if (!postgresContainer.isRunning()) {
            postgresContainer.start();
        }

        return postgresContainer;
    }

    /**
     * Clean up all containers (call in @AfterAll)
     */
    public static void stopAll() {
        if (postgresContainer != null && postgresContainer.isRunning()) {
            postgresContainer.stop();
        }
    }
}
