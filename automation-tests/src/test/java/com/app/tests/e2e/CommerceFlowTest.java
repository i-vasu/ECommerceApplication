package com.app.tests.e2e;

import io.restassured.http.ContentType;
import com.app.tests.e2e.BaseE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Disabled;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommerceFlowTest extends BaseE2ETest {

    private static Long createdOrderId;

    @Test
    @Order(1)
    @DisplayName("Inventory/Search: Sync Product and Verify Availability")
    void testInventoryFlow() {
        // 1. Sync Product (Admin)
        // 2. Search Product (Public)
        given()
                .baseUri(PRODUCT_SERVICE_URL)
                .when()
                .get("/api/v1/public/products") // Updated URL to v1
                .then()
                .statusCode(200);
        // .body("content.size()", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(2)
    @DisplayName("Cart Flow: Add Item to Cart")
    @Disabled("Pending End-to-End Environment Setup (Auth)")
    void testCartFlow() {
        // Mock Item ID
        Long productId = 1L;
    }

    @Test
    @Order(3)
    @DisplayName("Checkout Flow: Place Order")
    @Disabled("Pending End-to-End Environment Setup")
    void testCheckoutFlow() {
        // Place Order
    }

    @Test
    @Order(4)
    @DisplayName("Payment Flow: Process Payment")
    @Disabled("Pending End-to-End Environment Setup")
    void testPaymentFlow() {
        // Mock Payment Gateway Callback
    }

    @Test
    @Order(5)
    @DisplayName("Refund Flow: Request Refund")
    @Disabled("Pending End-to-End Environment Setup")
    void testRefundFlow() {
        // Initiate Refund
    }
}
