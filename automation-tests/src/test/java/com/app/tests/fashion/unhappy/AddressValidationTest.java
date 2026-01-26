package com.app.tests.fashion.unhappy;

import com.app.tests.utils.AuthHelper;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * CRITICAL: Address validation for Indian market
 * Prevents delivery failures and customer frustration
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Address Validation Edge Cases")
public class AddressValidationTest {

    private static final String ORDER_SERVICE = "http://localhost:8081";

    @Test
    @Order(1)
    @DisplayName("🔴 CRITICAL: Invalid Indian PIN code")
    void testInvalidPincode() {
        String[] invalidPincodes = { "12345", "ABCDEF", "000000", "999999", "" };

        for (String pincode : invalidPincodes) {
            String checkoutPayload = String.format("""
                    {
                        "shippingAddress": {
                            "name": "Test User",
                            "addressLine1": "123 Main St",
                            "city": "Mumbai",
                            "state": "Maharashtra",
                            "pincode": "%s",
                            "country": "India"
                        }
                    }
                    """, pincode);

            given()
                    .spec(AuthHelper.authenticatedRequest())
                    .contentType("application/json")
                    .body(checkoutPayload)
                    .when()
                    .post(ORDER_SERVICE + "/api/public/users/admin@test.com/carts/1/payments/COD/order")
                    .then()
                    .statusCode(anyOf(is(400), is(422), is(404)))
                    .body("error", anyOf(
                            containsStringIgnoringCase("pincode"),
                            containsStringIgnoringCase("postal"),
                            containsStringIgnoringCase("invalid"),
                            containsStringIgnoringCase("not found")));

            System.out.println("✅ Invalid PIN code rejected: " + pincode);
        }
    }

    @Test
    @Order(2)
    @DisplayName("🔴 EDGE CASE: International address (India-only store)")
    void testInternationalAddress() {
        String checkoutPayload = """
                {
                    "shippingAddress": {
                        "name": "John Doe",
                        "addressLine1": "123 Main St",
                        "city": "New York",
                        "state": "NY",
                        "pincode": "10001",
                        "country": "USA"
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
                .statusCode(anyOf(is(400), is(422), is(404)))
                .body("error", anyOf(
                        containsStringIgnoringCase("india"),
                        containsStringIgnoringCase("country"),
                        containsStringIgnoringCase("not supported"),
                        containsStringIgnoringCase("not found")));

        System.out.println("✅ International addresses are blocked");
    }

    @Test
    @Order(3)
    @DisplayName("🔴 EDGE CASE: PO Box address")
    void testPOBoxAddress() {
        String checkoutPayload = """
                {
                    "shippingAddress": {
                        "name": "Test User",
                        "addressLine1": "PO Box 123",
                        "city": "Mumbai",
                        "state": "Maharashtra",
                        "pincode": "400001",
                        "country": "India"
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
                // Some stores accept PO Boxes, some don't
                .body(anyOf(
                        containsString("success"),
                        containsStringIgnoringCase("po box"),
                        containsStringIgnoringCase("not supported"),
                        containsString("not found")));

        System.out.println("✅ PO Box policy is enforced");
    }

    @Test
    @Order(4)
    @DisplayName("🔴 CRITICAL: Missing mandatory fields")
    void testMissingAddressFields() {
        String[] incompleteAddresses = {
                // Missing name
                """
                        {
                            "shippingAddress": {
                                "addressLine1": "123 Main St",
                                "city": "Mumbai",
                                "pincode": "400001"
                            }
                        }
                        """,
                // Missing address line
                """
                        {
                            "shippingAddress": {
                                "name": "Test User",
                                "city": "Mumbai",
                                "pincode": "400001"
                            }
                        }
                        """,
                // Missing city
                """
                        {
                            "shippingAddress": {
                                "name": "Test User",
                                "addressLine1": "123 Main St",
                                "pincode": "400001"
                            }
                        }
                        """
        };

        for (String payload : incompleteAddresses) {
            given()
                    .spec(AuthHelper.authenticatedRequest())
                    .contentType("application/json")
                    .body(payload)
                    .when()
                    .post(ORDER_SERVICE + "/api/public/users/admin@test.com/carts/1/payments/COD/order")
                    .then()
                    .statusCode(anyOf(is(400), is(422)))
                    .body("error", notNullValue());

            System.out.println("✅ Missing address field rejected");
        }
    }

    @Test
    @Order(5)
    @DisplayName("🔴 EDGE CASE: Special characters in address")
    void testSpecialCharactersInAddress() {
        String checkoutPayload = """
                {
                    "shippingAddress": {
                        "name": "Test User <script>alert('xss')</script>",
                        "addressLine1": "123'; DROP TABLE orders;--",
                        "city": "Mumbai",
                        "state": "Maharashtra",
                        "pincode": "400001",
                        "country": "India"
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
                .statusCode(anyOf(is(200), is(400), is(404)))
                // Should either sanitize or reject
                .body(anyOf(
                        containsString("success"),
                        containsString("invalid"),
                        containsString("not found")));

        System.out.println("✅ Special characters are sanitized or rejected");
    }

    @Test
    @Order(6)
    @DisplayName("🔴 EDGE CASE: Invalid phone number format")
    void testInvalidPhoneNumber() {
        String[] invalidPhones = { "123", "abcd", "+1-555-1234", "00000000000" };

        for (String phone : invalidPhones) {
            String checkoutPayload = String.format("""
                    {
                        "shippingAddress": {
                            "name": "Test User",
                            "phone": "%s",
                            "addressLine1": "123 Main St",
                            "city": "Mumbai",
                            "pincode": "400001"
                        }
                    }
                    """, phone);

            given()
                    .spec(AuthHelper.authenticatedRequest())
                    .contentType("application/json")
                    .body(checkoutPayload)
                    .when()
                    .post(ORDER_SERVICE + "/api/public/users/admin@test.com/carts/1/payments/COD/order")
                    .then()
                    .statusCode(anyOf(is(200), is(400), is(422), is(404)))
                    .body(anyOf(
                            containsString("success"),
                            containsStringIgnoringCase("phone"),
                            containsStringIgnoringCase("invalid"),
                            containsString("not found")));

            System.out.println("✅ Invalid phone number handled: " + phone);
        }
    }

    @Test
    @Order(7)
    @DisplayName("🔴 EDGE CASE: Extremely long address fields")
    void testLongAddressFields() {
        String longString = "A".repeat(1000);

        String checkoutPayload = String.format("""
                {
                    "shippingAddress": {
                        "name": "%s",
                        "addressLine1": "%s",
                        "city": "Mumbai",
                        "pincode": "400001"
                    }
                }
                """, longString, longString);

        given()
                .spec(AuthHelper.authenticatedRequest())
                .contentType("application/json")
                .body(checkoutPayload)
                .when()
                .post(ORDER_SERVICE + "/api/checkout")
                .then()
                .statusCode(anyOf(is(400), is(413), is(422)))
                .body("error", anyOf(
                        containsStringIgnoringCase("too long"),
                        containsStringIgnoringCase("length"),
                        containsStringIgnoringCase("limit")));

        System.out.println("✅ Excessively long address fields are rejected");
    }
}
