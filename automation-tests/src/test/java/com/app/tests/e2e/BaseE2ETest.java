package com.app.tests.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public class BaseE2ETest {

    protected static final String ORDER_SERVICE_URL = "http://localhost:8081";
    protected static final String PRODUCT_SERVICE_URL = "http://localhost:8082";
    protected static final String MARKETPLACE_SERVICE_URL = "http://localhost:8083";

    @BeforeAll
    public static void setup() {
        // Default configuration if needed
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
