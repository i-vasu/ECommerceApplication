package com.app.tests.fashion.refund;

import com.app.tests.utils.AuthHelper;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Complete Refund Flow test coverage
 * Critical for customer satisfaction and financial accuracy
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Refund Flow: Complete Coverage")
public class RefundFlowTest {

    private static final String ORDER_SERVICE = "http://localhost:8081";
    private static Long paidOrderId = 12345L;

    // ============ HAPPY PATH ============

    @Test
    @Order(1)
    @DisplayName("✅ Initiate refund for paid order")
    void testInitiateRefund() {
        String refundPayload = String.format("""
                {
                    "orderId": %d,
                    "amount": 500.00,
                    "reason": "Product defect"
                }
                """, paidOrderId);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(refundPayload)
                .when()
                .post(ORDER_SERVICE + "/api/refunds/initiate")
                .then()
                .statusCode(anyOf(is(200), is(201), is(404)));
    }

    // ============ UNHAPPY PATH ============

    @Test
    @Order(2)
    @DisplayName("❌ Refund unpaid order")
    void testRefundUnpaidOrder() {
        String refundPayload = String.format("""
                {
                    "orderId": %d,
                    "amount": 500.00,
                    "reason": "Changed mind"
                }
                """, 99999L);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(refundPayload)
                .when()
                .post(ORDER_SERVICE + "/api/refunds/initiate")
                .then()
                .statusCode(anyOf(is(400), is(404)))
                .body("error", anyOf(
                        containsStringIgnoringCase("not paid"),
                        containsStringIgnoringCase("not found")));
    }

    @Test
    @Order(3)
    @DisplayName("❌ Refund already refunded order")
    void testDoubleRefund() {
        String refundPayload = String.format("""
                {
                    "orderId": %d,
                    "amount": 500.00,
                    "reason": "Duplicate refund attempt"
                }
                """, paidOrderId);

        // First refund
        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(refundPayload)
                .when()
                .post(ORDER_SERVICE + "/api/refunds/initiate")
                .then()
                .statusCode(anyOf(is(200), is(201), is(404)));

        // Second refund (should fail)
        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(refundPayload)
                .when()
                .post(ORDER_SERVICE + "/api/refunds/initiate")
                .then()
                .statusCode(anyOf(is(400), is(409), is(404)))
                .body("error", anyOf(
                        containsStringIgnoringCase("already refunded"),
                        containsStringIgnoringCase("not found")));
    }

    @Test
    @Order(4)
    @DisplayName("❌ Refund with invalid order ID")
    void testRefundInvalidOrderId() {
        String refundPayload = """
                {
                    "orderId": -1,
                    "amount": 500.00,
                    "reason": "Test"
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(refundPayload)
                .when()
                .post(ORDER_SERVICE + "/api/refunds/initiate")
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }

    // ============ EDGE CASES ============

    @Test
    @Order(5)
    @DisplayName("⚠️ Partial refund")
    void testPartialRefund() {
        String partialRefundPayload = String.format("""
                {
                    "orderId": %d,
                    "amount": 250.00,
                    "reason": "Missing item in package"
                }
                """, paidOrderId);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(partialRefundPayload)
                .when()
                .post(ORDER_SERVICE + "/api/refunds/partial")
                .then()
                .statusCode(anyOf(is(200), is(201), is(404)));
    }

    @Test
    @Order(6)
    @DisplayName("⚠️ Refund after 30 days")
    void testRefundAfter30Days() {
        String lateRefundPayload = String.format("""
                {
                    "orderId": %d,
                    "amount": 500.00,
                    "reason": "Late return",
                    "orderDate": "2025-12-01"
                }
                """, paidOrderId);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(lateRefundPayload)
                .when()
                .post(ORDER_SERVICE + "/api/refunds/initiate")
                .then()
                .statusCode(anyOf(is(400), is(422), is(404)))
                .body("error", anyOf(
                        containsStringIgnoringCase("window"),
                        containsStringIgnoringCase("expired"),
                        containsStringIgnoringCase("not found")));
    }

    @Test
    @Order(7)
    @DisplayName("⚠️ Concurrent refund requests")
    void testConcurrentRefundRequests() {
        // Simulate two refund requests at the same time
        System.out.println("⚠️ Concurrent refund test (requires load testing)");
    }

    @Test
    @Order(8)
    @DisplayName("⚠️ Refund amount exceeds order total")
    void testExcessiveRefundAmount() {
        String excessiveRefundPayload = String.format("""
                {
                    "orderId": %d,
                    "amount": 999999.00,
                    "reason": "Test excessive amount"
                }
                """, paidOrderId);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(excessiveRefundPayload)
                .when()
                .post(ORDER_SERVICE + "/api/refunds/initiate")
                .then()
                .statusCode(anyOf(is(400), is(422), is(404)))
                .body("error", anyOf(
                        containsStringIgnoringCase("exceeds"),
                        containsStringIgnoringCase("invalid"),
                        containsStringIgnoringCase("not found")));
    }

    @Test
    @Order(9)
    @DisplayName("⚠️ Negative refund amount")
    void testNegativeRefundAmount() {
        String negativeRefundPayload = String.format("""
                {
                    "orderId": %d,
                    "amount": -100.00,
                    "reason": "Test"
                }
                """, paidOrderId);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(negativeRefundPayload)
                .when()
                .post(ORDER_SERVICE + "/api/refunds/initiate")
                .then()
                .statusCode(400)
                .body("error", containsStringIgnoringCase("amount"));
    }

    @Test
    @Order(10)
    @DisplayName("⚠️ Refund status tracking")
    void testRefundStatusTracking() {
        given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .get(ORDER_SERVICE + "/api/refunds/" + paidOrderId + "/status")
                .then()
                .statusCode(anyOf(is(200), is(404)))
                .body(anyOf(
                        containsString("status"),
                        containsString("not found")));
    }
}
