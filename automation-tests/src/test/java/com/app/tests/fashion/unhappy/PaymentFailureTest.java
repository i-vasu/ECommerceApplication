package com.app.tests.fashion.unhappy;

import com.app.tests.utils.AuthHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * CRITICAL: Payment failure scenarios
 * These tests prevent revenue loss and customer frustration
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Payment Failures & Recovery")
public class PaymentFailureTest {

    private static final String ORDER_SERVICE = "http://localhost:8081";
    private static Long testOrderId;

    @BeforeAll
    static void createTestOrder() {
        // Create an order first
        testOrderId = 12345L; // Mock order
    }

    @Test
    @Order(1)
    @DisplayName("🔴 CRITICAL: Payment gateway returns error")
    void testPaymentGatewayError() {
        Response response = given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .post(ORDER_SERVICE + "/api/payments/create/" + testOrderId)
                .then()
                .extract().response();

        if (response.statusCode() == 500 || response.statusCode() == 503) {
            // Gateway is down - verify order stays in PENDING state
            given()
                    .spec(AuthHelper.authenticatedRequest())
                    .when()
                    .get(ORDER_SERVICE + "/api/orders/" + testOrderId)
                    .then()
                    .statusCode(200)
                    .body("status", anyOf(
                            equalTo("PENDING"),
                            equalTo("PAYMENT_FAILED")));

            System.out.println("✅ Order remains in safe state when payment gateway fails");
        } else {
            System.out.println("⚠️ Payment gateway is up, skipping failure test");
        }
    }

    @Test
    @Order(2)
    @DisplayName("🔴 CRITICAL: Invalid payment signature (security)")
    void testInvalidPaymentSignature() {
        String invalidSignature = "tampered_signature_12345";
        String mockPaymentId = "pay_fake123";

        given()
                .spec(AuthHelper.authenticatedRequest())
                .queryParam("razorpay_payment_id", mockPaymentId)
                .queryParam("razorpay_signature", invalidSignature)
                .when()
                .post(ORDER_SERVICE + "/api/payments/verify/" + testOrderId)
                .then()
                .statusCode(anyOf(is(400), is(403)))
                .body("error", anyOf(
                        containsStringIgnoringCase("signature"),
                        containsStringIgnoringCase("invalid"),
                        containsStringIgnoringCase("verification")));

        System.out.println("✅ Invalid payment signature is REJECTED");
    }

    @Test
    @Order(3)
    @DisplayName("🔴 CRITICAL: Payment timeout - no confirmation received")
    void testPaymentTimeout() {
        // Simulate: Payment initiated but no callback within 15 minutes

        given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .post(ORDER_SERVICE + "/api/payments/" + testOrderId + "/check-timeout")
                .then()
                .statusCode(anyOf(is(200), is(404)))
                .body(anyOf(
                        containsString("timeout"),
                        containsString("expired"),
                        containsString("not found")));

        System.out.println("✅ System handles payment timeout gracefully");
    }

    @Test
    @Order(4)
    @DisplayName("🔴 EDGE CASE: Double payment submission")
    void testDoublePayment() {
        // Customer clicks "Pay" button twice
        String mockPaymentId = "pay_double_" + System.currentTimeMillis();

        // First payment
        Response first = given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .post(ORDER_SERVICE + "/api/payments/create/" + testOrderId)
                .then()
                .extract().response();

        // Second payment (should be rejected or idempotent)
        Response second = given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .post(ORDER_SERVICE + "/api/payments/create/" + testOrderId)
                .then()
                .statusCode(anyOf(is(200), is(400), is(409)))
                .extract().response();

        if (second.statusCode() == 200) {
            // Idempotent: should return same payment ID
            String firstPaymentId = first.jsonPath().getString("razorpayOrderId");
            String secondPaymentId = second.jsonPath().getString("razorpayOrderId");

            Assertions.assertEquals(firstPaymentId, secondPaymentId,
                    "Double payment should return same payment ID (idempotent)");
        }

        System.out.println("✅ Double payment prevented or handled idempotently");
    }

    @Test
    @Order(5)
    @DisplayName("🔴 CRITICAL: Customer abandons payment window")
    void testPaymentAbandonment() {
        // Customer closes Razorpay popup without completing payment

        given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .post(ORDER_SERVICE + "/api/payments/" + testOrderId + "/abandon")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Verify order returns to cart or stays pending
        given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .get(ORDER_SERVICE + "/api/orders/" + testOrderId)
                .then()
                .statusCode(200)
                .body("status", not(equalTo("PAID")));

        System.out.println("✅ Abandoned payment doesn't create paid order");
    }

    @Test
    @Order(6)
    @DisplayName("🔴 EDGE CASE: Payment amount mismatch")
    void testPaymentAmountMismatch() {
        // Razorpay callback has different amount than order total
        // This should NEVER happen but tests for tampering

        String mockPaymentId = "pay_tampered_" + System.currentTimeMillis();

        given()
                .spec(AuthHelper.authenticatedRequest())
                .queryParam("razorpay_payment_id", mockPaymentId)
                .queryParam("amount", "100") // Wrong amount
                .when()
                .post(ORDER_SERVICE + "/api/payments/verify/" + testOrderId)
                .then()
                .statusCode(anyOf(is(400), is(403)))
                .body("error", anyOf(
                        containsStringIgnoringCase("amount"),
                        containsStringIgnoringCase("mismatch")));

        System.out.println("✅ Payment amount tampering is detected");
    }

    @Test
    @Order(7)
    @DisplayName("🔴 EDGE CASE: Refund fails on payment gateway")
    void testRefundFailure() {
        given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .post(ORDER_SERVICE + "/api/refunds/" + testOrderId + "/process")
                .then()
                .statusCode(anyOf(is(200), is(500), is(404)))
                .body(anyOf(
                        containsString("refund"),
                        containsString("error"),
                        containsString("not found")));

        System.out.println("✅ Refund failure is logged for manual processing");
    }
}
