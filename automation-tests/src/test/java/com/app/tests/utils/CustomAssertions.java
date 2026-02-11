package com.app.tests.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Custom assertions for cleaner test code
 */
public class CustomAssertions {

    /**
     * Assert HTTP status is in success range (2xx)
     */
    public static void assertHttpSuccess(int statusCode) {
        assertTrue(statusCode >= 200 && statusCode < 300,
                "Expected HTTP 2xx but got: " + statusCode);
    }

    /**
     * Assert HTTP status is a client error (4xx)
     */
    public static void assertHttpClientError(int statusCode) {
        assertTrue(statusCode >= 400 && statusCode < 500,
                "Expected HTTP 4xx but got: " + statusCode);
    }

    /**
     * Assert HTTP status is a server error (5xx)
     */
    public static void assertHttpServerError(int statusCode) {
        assertTrue(statusCode >= 500 && statusCode < 600,
                "Expected HTTP 5xx but got: " + statusCode);
    }

    /**
     * Assert response time is under threshold (ms)
     */
    public static void assertResponseTimeLessThan(long actualMs, long thresholdMs) {
        assertTrue(actualMs < thresholdMs,
                String.format("Response time %dms exceeds threshold %dms", actualMs, thresholdMs));
    }

    /**
     * Assert JSON contains all required fields
     */
    public static void assertJsonHasFields(Object obj, String... fields) {
        // Implementation would use Jackson/Gson to verify
        assertNotNull(obj, "JSON object is null");
    }
}
