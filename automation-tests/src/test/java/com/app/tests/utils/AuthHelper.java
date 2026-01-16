package com.app.tests.utils;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Authentication Helper for managing JWT tokens in tests
 */
public class AuthHelper {

    private static final String ORDER_SERVICE_URL = System.getProperty("order.service.url", "http://localhost:8081");
    private static String cachedToken;

    /**
     * Get a valid JWT token (cached for performance)
     */
    public static String getValidToken() {
        if (cachedToken != null) {
            return cachedToken;
        }

        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@test.com");
        loginRequest.put("password", "admin123");

        Response response = given()
                .contentType("application/json")
                .body(loginRequest)
                .when()
                .post(ORDER_SERVICE_URL + "/api/login");

        if (response.statusCode() == 200) {
            cachedToken = response.jsonPath().getString("token");
        } else {
            // Fallback: generate a mock token for testing
            cachedToken = generateMockToken();
        }

        return cachedToken;
    }

    /**
     * Create an authenticated request with Bearer token
     */
    public static RequestSpecification authenticatedRequest() {
        return given()
                .header("Authorization", "Bearer " + getValidToken());
    }

    /**
     * Generate a mock JWT for isolated testing
     */
    private static String generateMockToken() {
        // This is a placeholder. In production, use a proper JWT library
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlRlc3QgVXNlciIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    }

    /**
     * Clear cached token (useful between test suites)
     */
    public static void clearToken() {
        cachedToken = null;
    }

    /**
     * Get an expired token for testing token expiry scenarios
     */
    public static String getExpiredToken() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.4Adcj0vJWfQbMBRUU9bKNfUmV0YMETJSiYPRiYbGe_8";
    }

    /**
     * Get a malformed token for testing validation
     */
    public static String getMalformedToken() {
        return "invalid.token.here";
    }
}
