package com.app.tests.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class SecurityRegressionTest extends BaseE2ETest {

    @Test
    @DisplayName("Product Service - Public Endpoints should be accessible")
    void testProductPublicAccess() {
        given()
                .baseUri(PRODUCT_SERVICE_URL)
                .when()
                .get("/api/public/products")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Product Service - Admin Endpoints should be secured")
    void testProductAdminSecurity() {
        given()
                .baseUri(PRODUCT_SERVICE_URL)
                .when()
                .post("/api/admin/products/sync")
                .then()
                .statusCode(anyOf(is(401), is(403))); // 401 Unauthorized or 403 Forbidden
    }

    @Test
    @DisplayName("Marketplace Service - Webhooks should be accessible (but signature verified)")
    void testMarketplaceWebhookAccess() {
        // Expect 401/403/400 due to invalid signature, BUT NOT because of JWT (Missing
        // Bearer)
        // If JWT filter blocked it, it would be strictly 401/403.
        // We verify that the service validates signature (which implies reachable).

        given()
                .baseUri(MARKETPLACE_SERVICE_URL)
                .header("X-Marketplace-Signature", "invalid-sig")
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/webhooks/amazon")
                .then()
                // 200: Signature ignored (bad)
                // 400/403: Signature rejected (good)
                // 401: JWT rejected (bad - webhook should be public)
                .statusCode(anyOf(is(400), is(403)));
    }

    @Test
    @DisplayName("Marketplace Service - Admin Endpoints should be secured")
    void testMarketplaceAdminSecurity() {
        given()
                .baseUri(MARKETPLACE_SERVICE_URL)
                .when()
                .get("/api/marketplace/config")
                .then()
                .statusCode(401);
    }
}
