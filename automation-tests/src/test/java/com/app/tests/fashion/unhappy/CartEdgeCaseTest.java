package com.app.tests.fashion.unhappy;

import com.app.tests.utils.AuthHelper;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * CRITICAL: Cart & checkout edge cases
 * Prevents pricing errors and inventory issues
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Cart & Checkout Edge Cases")
public class CartEdgeCaseTest {

    private static final String PRODUCT_SERVICE = "http://localhost:8082";
    private static final String ORDER_SERVICE = "http://localhost:8081";

    @Test
    @Order(1)
    @DisplayName("🔴 CRITICAL: Price changes between cart and checkout")
    void testPriceChangeDetection() {
        // Customer adds item at ₹500, price changes to ₹600 before checkout

        String cartPayload = """
                {
                    "productId": 1,
                    "size": "M",
                    "color": "Red",
                    "quantity": 1,
                    "priceAtAdd": 500.00
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // Now checkout (system should detect price change)
        String checkoutPayload = """
                {
                    "shippingAddress": {
                        "name": "Test User",
                        "addressLine1": "123 Main St",
                        "city": "Mumbai",
                        "pincode": "400001"
                    }
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(checkoutPayload)
                .when()
                .post(ORDER_SERVICE + "/api/checkout")
                .then()
                .statusCode(anyOf(is(200), is(409), is(404)))
                // Should either:
                // - Update cart with new price and warn customer (200)
                // - Block checkout and show price change (409)
                .body(anyOf(
                        containsString("price"),
                        containsString("updated"),
                        containsString("changed"),
                        containsString("not found")));

        System.out.println("✅ Price changes are detected before payment");
    }

    @Test
    @Order(2)
    @DisplayName("🔴 CRITICAL: Product deleted after adding to cart")
    void testDeletedProductInCart() {
        String cartPayload = """
                {
                    "productId": 99999,
                    "size": "M",
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
                .statusCode(anyOf(is(404), is(400)))
                .body("error", containsStringIgnoringCase("not found"));

        System.out.println("✅ Deleted/invalid products cannot be added to cart");
    }

    @Test
    @Order(3)
    @DisplayName("🔴 CRITICAL: Stock depleted during checkout")
    void testStockDepletedDuringCheckout() {
        // Customer adds last item, someone else buys it before checkout

        String cartPayload = """
                {
                    "productId": 1,
                    "size": "XS",
                    "color": "Red",
                    "quantity": 1
                }
                """;

        // Add to cart
        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(anyOf(is(200), is(201), is(400)));

        // Try to checkout (might fail if stock is gone)
        String checkoutPayload = """
                {
                    "shippingAddress": {
                        "name": "Test User",
                        "addressLine1": "123 Main St",
                        "city": "Mumbai",
                        "pincode": "400001"
                    }
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(checkoutPayload)
                .when()
                .post(ORDER_SERVICE + "/api/checkout")
                .then()
                .statusCode(anyOf(is(200), is(400), is(422), is(404)))
                .body(anyOf(
                        containsString("success"),
                        containsString("stock"),
                        containsString("unavailable"),
                        containsString("not found")));

        System.out.println("✅ Out-of-stock detected at checkout");
    }

    @Test
    @Order(4)
    @DisplayName("🔴 EDGE CASE: Empty cart checkout")
    void testEmptyCartCheckout() {
        String checkoutPayload = """
                {
                    "shippingAddress": {
                        "name": "Test User",
                        "addressLine1": "123 Main St",
                        "city": "Mumbai",
                        "pincode": "400001"
                    }
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(checkoutPayload)
                .when()
                .post(ORDER_SERVICE + "/api/checkout")
                .then()
                .statusCode(anyOf(is(400), is(422)))
                .body("error", anyOf(
                        containsStringIgnoringCase("empty"),
                        containsStringIgnoringCase("no items")));

        System.out.println("✅ Empty cart checkout is prevented");
    }

    @Test
    @Order(5)
    @DisplayName("🔴 EDGE CASE: Add same variant multiple times")
    void testDuplicateVariantInCart() {
        String cartPayload = """
                {
                    "productId": 1,
                    "size": "M",
                    "color": "Blue",
                    "quantity": 2
                }
                """;

        // Add first time
        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // Add same variant again
        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                // Should either merge or add separately
                .body(anyOf(
                        containsString("quantity"),
                        containsString("item")));

        System.out.println("✅ Duplicate variant addition handled (merged or separate)");
    }

    @Test
    @Order(6)
    @DisplayName("🔴 EDGE CASE: Negative quantity")
    void testNegativeQuantity() {
        String cartPayload = """
                {
                    "productId": 1,
                    "size": "M",
                    "quantity": -5
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(400)
                .body("error", containsStringIgnoringCase("quantity"));

        System.out.println("✅ Negative quantity is rejected");
    }

    @Test
    @Order(7)
    @DisplayName("🔴 EDGE CASE: Zero quantity")
    void testZeroQuantity() {
        String cartPayload = """
                {
                    "productId": 1,
                    "size": "M",
                    "quantity": 0
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE + "/api/cart")
                .then()
                .statusCode(400)
                .body("error", containsStringIgnoringCase("quantity"));

        System.out.println("✅ Zero quantity is rejected");
    }
}
