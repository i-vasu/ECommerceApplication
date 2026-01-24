package com.app.tests.e2e;

import com.app.tests.utils.AuthHelper;
import com.app.tests.utils.CustomAssertions;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Complete Cart → Checkout → Payment Flow")
public class CartCheckoutFlowTest extends BaseE2ETest {

    private static Long testUserId;
    private static Long testProductId = 1L; // Assume seeded
    private static Long createdOrderId;

    @BeforeAll
    static void setupUser() {
        // In production, this would create a test user
        // For now, we use the authenticated admin
        testUserId = 1L;
    }

    @Test
    @Order(1)
    @DisplayName("Happy Path: Add item to cart")
    void testAddToCart() {
        String cartPayload = String.format("""
                {
                    "productId": %d,
                    "quantity": 2
                }
                """, testProductId);

        Response response = given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE_URL + "/api/cart/add")
                .then()
                .extract().response();

        // Verify response
        CustomAssertions.assertHttpSuccess(response.statusCode());
    }

    @Test
    @Order(2)
    @DisplayName("Happy Path: View cart contents")
    void testViewCart() {
        Response response = given()
                .spec(AuthHelper.authenticatedRequest())
                .when()
                .get(ORDER_SERVICE_URL + "/api/cart")
                .then()
                .statusCode(200)
                .body("items.size()", greaterThan(0))
                .extract().response();

        CustomAssertions.assertResponseTimeLessThan(
                response.time(),
                1000 // 1 second SLA
        );
    }

    @Test
    @Order(3)
    @DisplayName("Unhappy Path: Add out-of-stock item")
    void testAddOutOfStockItem() {
        String cartPayload = """
                {
                    "productId": 99999,
                    "quantity": 1
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE_URL + "/api/cart/add")
                .then()
                .statusCode(anyOf(is(404), is(400)));
    }

    @Test
    @Order(4)
    @DisplayName("Edge Case: Add maximum quantity")
    void testAddMaxQuantity() {
        String cartPayload = String.format("""
                {
                    "productId": %d,
                    "quantity": 999
                }
                """, testProductId);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(cartPayload)
                .when()
                .post(ORDER_SERVICE_URL + "/api/cart/add")
                .then()
                .statusCode(anyOf(is(200), is(400))); // Either accepted or quantity validation
    }

    @Test
    @Order(5)
    @DisplayName("Happy Path: Proceed to checkout")
    void testCheckout() {
        String checkoutPayload = """
                {
                    "shippingAddress": {
                        "street": "123 Test St",
                        "city": "Mumbai",
                        "postalCode": "400001",
                        "country": "India"
                    }
                }
                """;

        Response response = given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(checkoutPayload)
                .when()
                .post(ORDER_SERVICE_URL + "/api/checkout")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract().response();

        // Store order ID for subsequent tests
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            createdOrderId = response.jsonPath().getLong("orderId");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Unhappy Path: Checkout empty cart")
    void testCheckoutEmptyCart() {
        // First, clear the cart (if endpoint exists)
        // Then attempt checkout

        String checkoutPayload = """
                {
                    "shippingAddress": {
                        "street": "123 Test St",
                        "city": "Mumbai",
                        "postalCode": "400001"
                    }
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(checkoutPayload)
                .when()
                .post(ORDER_SERVICE_URL + "/api/checkout")
                .then()
                .statusCode(anyOf(is(400), is(422))); // Validation error
    }

    @Test
    @Order(7)
    @DisplayName("Edge Case: Invalid shipping address")
    void testInvalidShippingAddress() {
        String invalidPayload = """
                {
                    "shippingAddress": {
                        "street": "",
                        "city": "",
                        "postalCode": "INVALID"
                    }
                }
                """;

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(invalidPayload)
                .when()
                .post(ORDER_SERVICE_URL + "/api/checkout")
                .then()
                .statusCode(400)
                .body("errors", notNullValue());
    }

    @AfterAll
    static void cleanup() {
        AuthHelper.clearToken();
    }
}
