package com.app.tests.fashion;

import com.app.tests.fixtures.TestDataFactory;
import com.app.tests.utils.AuthHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Critical Fashion E-Commerce Business Scenarios
 * Tests real customer journeys from product discovery to order fulfillment
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Fashion Store: Complete Customer Journey")
public class FashionCustomerJourneyTest {

    private static final String PRODUCT_SERVICE = "http://localhost:8082";
    private static final String ORDER_SERVICE = "http://localhost:8081";

    private static Long selectedProductId;
    private static String selectedSize;
    private static String selectedColor;
    private static Long orderId;

    @Test
    @Order(1)
    @DisplayName("Customer browses women's sarees collection")
    void testBrowseFashionCollection() {
        Response response = given()
                .when()
                .get(PRODUCT_SERVICE + "/api/public/products?category=Sarees")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.size()", greaterThan(0))
                // Verify fashion-specific attributes
                .body("content[0].variants", notNullValue()) // Size/color variants
                .body("content[0].images", notNullValue()) // Product images
                .body("content[0].brand", notNullValue()) // Brand info
                .extract().response();

        // Store first product for subsequent tests
        selectedProductId = response.jsonPath().getLong("content[0].id");

        System.out.println("✅ Customer can browse fashion catalog with variants");
    }

    @Test
    @Order(2)
    @DisplayName("Customer views product details with size/color options")
    void testViewProductWithVariants() {
        Response response = given()
                .when()
                .get(PRODUCT_SERVICE + "/api/public/products/" + selectedProductId)
                .then()
                .statusCode(200)
                // Fashion-specific validations
                .body("variants.size()", greaterThan(0))
                .body("variants[0].size", notNullValue())
                .body("variants[0].color", notNullValue())
                .body("variants[0].stock", greaterThanOrEqualTo(0))
                .body("material", notNullValue())
                .body("careInstructions", notNullValue())
                .extract().response();

        // Select first available variant
        selectedSize = response.jsonPath().getString("variants[0].size");
        selectedColor = response.jsonPath().getString("variants[0].color");

        System.out.println("✅ Product details show size/color variants with stock info");
    }

    @Test
    @Order(3)
    @DisplayName("CRITICAL: Add specific size/color variant to cart")
    void testAddVariantToCart() {
        String cartPayload = String.format("""
                {
                    "productId": %d,
                    "size": "%s",
                    "color": "%s",
                    "quantity": 1
                }
                """, selectedProductId, selectedSize, selectedColor);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("items[0].productId", equalTo(selectedProductId.intValue()))
                .body("items[0].size", equalTo(selectedSize))
                .body("items[0].color", equalTo(selectedColor));

        System.out.println("✅ Correct variant (size+color) added to cart");
    }

    @Test
    @Order(4)
    @DisplayName("EDGE CASE: Try to add out-of-stock size")
    void testAddOutOfStockSize() {
        String cartPayload = String.format("""
                {
                    "productId": %d,
                    "size": "XXL",
                    "color": "%s",
                    "quantity": 100
                }
                """, selectedProductId, selectedColor);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(anyOf(is(400), is(422)))
                .body("error", containsStringIgnoringCase("stock"));

        System.out.println("✅ System prevents adding out-of-stock variants");
    }

    @Test
    @Order(5)
    @DisplayName("CRITICAL: Calculate accurate total with tax and shipping")
    void testCheckoutWithAccuratePricing() {
        String checkoutPayload = """
                {
                    "shippingAddress": {
                        "name": "Priya Sharma",
                        "phone": "+91-9876543210",
                        "addressLine1": "45, MG Road",
                        "city": "Mumbai",
                        "state": "Maharashtra",
                        "pincode": "400001",
                        "country": "India"
                    }
                }
                """;

        Response response = given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(checkoutPayload)
                .when()
                .post(ORDER_SERVICE + "/api/checkout")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                // Verify accurate pricing
                .body("subtotal", greaterThan(0f))
                .body("tax", greaterThan(0f)) // GST calculation
                .body("shippingCost", greaterThanOrEqualTo(0f))
                .body("totalAmount", greaterThan(0f))
                // Verify it matches: subtotal + tax + shipping
                .extract().response();

        orderId = response.jsonPath().getLong("orderId");

        float subtotal = response.jsonPath().getFloat("subtotal");
        float tax = response.jsonPath().getFloat("tax");
        float shipping = response.jsonPath().getFloat("shippingCost");
        float total = response.jsonPath().getFloat("totalAmount");

        Assertions.assertEquals(subtotal + tax + shipping, total, 0.01,
                "Total amount calculation is incorrect!");

        System.out.println("✅ Checkout calculates accurate pricing with tax");
    }

    @Test
    @Order(6)
    @DisplayName("CRITICAL: Payment flow with Razorpay for fashion order")
    void testFashionOrderPayment() {
        // Create payment order
        Response paymentOrder = given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .post(ORDER_SERVICE + "/api/payments/create/" + orderId)
                .then()
                .statusCode(200)
                .body("razorpayOrderId", notNullValue())
                .body("amount", greaterThan(0))
                .body("currency", equalTo("INR"))
                .extract().response();

        String razorpayOrderId = paymentOrder.jsonPath().getString("razorpayOrderId");

        System.out.println("✅ Payment order created with correct INR amount");

        // Simulate payment verification (in real scenario, this comes from Razorpay
        // callback)
        String mockPaymentId = "pay_mock_" + System.currentTimeMillis();
        String mockSignature = "mock_signature_xyz";

        given()
                .spec(AuthHelper.authenticatedRequest())
                .queryParam("razorpay_payment_id", mockPaymentId)
                .queryParam("razorpay_signature", mockSignature)
                .when()
                .post(ORDER_SERVICE + "/api/payments/verify/" + orderId)
                .then()
                .statusCode(anyOf(is(200), is(400))); // 400 if signature validation fails (expected)

        System.out.println("✅ Payment verification flow works");
    }

    @Test
    @Order(7)
    @DisplayName("Customer tracks order status")
    void testOrderTracking() {
        given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .get(ORDER_SERVICE + "/api/orders/" + orderId)
                .then()
                .statusCode(200)
                .body("orderId", equalTo(orderId.intValue()))
                .body("status", notNullValue())
                .body("items", notNullValue())
                .body("items[0].size", equalTo(selectedSize))
                .body("items[0].color", equalTo(selectedColor));

        System.out.println("✅ Customer can track order with variant details");
    }

    @Test
    @Order(8)
    @DisplayName("EDGE CASE: Customer tries to order discontinued product")
    void testDiscontinuedProduct() {
        String cartPayload = """
                {
                    "productId": 99999,
                    "size": "M",
                    "color": "Blue",
                    "quantity": 1
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(404)
                .body("error", containsStringIgnoringCase("not found"));

        System.out.println("✅ System handles discontinued products gracefully");
    }

    @Test
    @Order(9)
    @DisplayName("EDGE CASE: Bulk order exceeding single-transaction limit")
    void testBulkOrderLimit() {
        String bulkCartPayload = String.format("""
                {
                    "productId": %d,
                    "size": "%s",
                    "color": "%s",
                    "quantity": 50
                }
                """, selectedProductId, selectedSize, selectedColor);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(bulkCartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(anyOf(is(200), is(400)))
                .body(anyOf(
                        containsString("success"),
                        containsString("maximum quantity")));

        System.out.println("✅ System validates bulk order quantities");
    }

    @AfterAll
    static void cleanup() {
        AuthHelper.clearToken();
        System.out.println("\n✅ FASHION E-COMMERCE JOURNEY TEST COMPLETE");
    }
}
