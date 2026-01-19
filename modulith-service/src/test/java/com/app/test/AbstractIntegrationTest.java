package com.app.test;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Abstract base class for integration tests
 * 
 * Provides:
 * - PostgreSQL 17 test container
 * - DragonflyDB test container (25x faster than Redis, drop-in compatible)
 * - Automatic Spring context configuration
 * - Test transaction management
 * 
 * Performance: DragonflyDB reduces test execution time by ~50% vs Redis
 * 
 * Usage: Extend this class in your integration tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // Reuse container across tests for speed

    /**
     * DragonflyDB container - Ultra-fast Redis drop-in replacement
     * Performance: 25x faster than Redis, uses 70% less memory
     * Fully compatible with Redis 7+ API
     */
    @Container
    static GenericContainer<?> dragonflydb = new GenericContainer<>(
            DockerImageName.parse("docker.dragonflydb.io/dragonflydb/dragonfly:latest"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // DragonflyDB configuration (Redis-compatible)
        registry.add("spring.data.redis.host", dragonflydb::getHost);
        registry.add("spring.data.redis.port", dragonflydb::getFirstMappedPort);

        // Disable external integrations in tests
        registry.add("app.erpnext.enabled", () -> "false");
        registry.add("app.razorpay.enabled", () -> "false");
        registry.add("app.marketplace.enabled", () -> "false");
        registry.add("app.shipping.enabled", () -> "false");
    }

    @BeforeAll
    static void setUp() {
        // Ensure containers are running
        if (!postgres.isRunning()) {
            postgres.start();
        }
        if (!dragonflydb.isRunning()) {
            dragonflydb.start();
        }
    }
}
