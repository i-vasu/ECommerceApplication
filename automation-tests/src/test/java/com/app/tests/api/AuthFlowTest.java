package com.app.tests.api;

import com.app.tests.e2e.BaseE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AuthFlowTest extends BaseE2ETest {

    @Test
    @DisplayName("Auth: Verify Login Endpoint Exists")
    void verifyLoginEndpoint() {
        given()
                .baseUri(ORDER_SERVICE_URL)
                .contentType("application/json")
                .body("{ \"username\": \"test\", \"password\": \"test\" }")
                .when()
                .post("/api/login")
                .then()
                // 200 (Mock success) or 401 (Bad credentials) check that service is reachable
                .statusCode(anyOf(is(200), is(401), is(403)));
    }
}
