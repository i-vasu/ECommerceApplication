package com.app.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;

/**
 * Abstract base class for integration tests
 * 
 * Provides:
 * - PostgreSQL 17 test container (Optional)
 * - DragonflyDB test container (Optional)
 * - Fallback to H2 and Local Redis if Docker is unavailable
 * - RestTestClient for REST API testing
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected RestTestClient restTestClient;

    static PostgreSQLContainer<?> postgres;
    static GenericContainer<?> dragonflydb;

    static {
        try {
            postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("ecommerce_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

            dragonflydb = new GenericContainer<>(
                    DockerImageName.parse("redis:alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

            postgres.start();
            dragonflydb.start();
        } catch (Throwable t) {
            System.err.println("Docker not available, falling back to H2: " + t.getMessage());
            postgres = null;
            dragonflydb = null;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

            if (dragonflydb != null && dragonflydb.isRunning()) {
                registry.add("spring.data.redis.host", dragonflydb::getHost);
                registry.add("spring.data.redis.port", dragonflydb::getFirstMappedPort);
            } else {
                registry.add("spring.data.redis.host", () -> "localhost");
                registry.add("spring.data.redis.port", () -> "6379");
            }
        } else {
            // H2 Fallback
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");

            registry.add("spring.data.redis.host", () -> "localhost");
            registry.add("spring.data.redis.port", () -> "6379");
        }

        registry.add("app.erpnext.enabled", () -> "false");
        registry.add("app.razorpay.enabled", () -> "false");
        registry.add("app.marketplace.enabled", () -> "false");
        registry.add("app.shipping.enabled", () -> "false");
    }
}
