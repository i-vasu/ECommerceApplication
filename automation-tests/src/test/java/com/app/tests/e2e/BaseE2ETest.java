package com.app.tests.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public class BaseE2ETest {

    protected static final String MONOLITH_URL = "http://localhost:8080";
    protected static final String ORDER_SERVICE_URL = MONOLITH_URL;
    protected static final String PRODUCT_SERVICE_URL = MONOLITH_URL;
    protected static final String MARKETPLACE_SERVICE_URL = MONOLITH_URL;

    @BeforeAll
    public static void setup() {
        // Default configuration if needed
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
