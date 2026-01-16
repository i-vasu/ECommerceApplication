package com.app.tests.smoke;

import com.app.tests.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class SmokeTest extends BaseE2ETest {

    @Test
    @DisplayName("Smoke: Critical Services UP")
    void criticalServicesUp() {
        // Order Service
        given().baseUri(ORDER_SERVICE_URL).get("/actuator/health")
                .then().statusCode(200).body("status", equalTo("UP"));

        // Product Service
        given().baseUri(PRODUCT_SERVICE_URL).get("/actuator/health")
                .then().statusCode(200).body("status", equalTo("UP"));

        // Marketplace Service
        given().baseUri(MARKETPLACE_SERVICE_URL).get("/actuator/health")
                .then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Smoke: Database Connectivity")
    void databaseConnectivity() {
        // Indirect check via a simple read endpoint
        given().baseUri(PRODUCT_SERVICE_URL).get("/api/public/products")
                .then().statusCode(200);
    }
}
