package com.app.tests.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class OrderFlowTest extends BaseE2ETest {

    @Test
    @DisplayName("Full Order Lifecycle: Create -> Payment -> Check")
    void testOrderLifecycle() {
        // 1. Login (Mock or assume Test User) - Skipping Login for now, assuming Order
        // Service might generate token
        // For this test, verifying Health/Structure first.

        // Check Health
        given()
                .baseUri(ORDER_SERVICE_URL)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));

        // Note: Full flow requires seeding data (Products).
        // This is a placeholder to be expanded once we confirm the test module works.
    }
}
