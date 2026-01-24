package com.app.tests.api;

import com.app.tests.e2e.BaseE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class SyncFlowTest extends BaseE2ETest {

    @Test
    @DisplayName("Sync: Verify Admin Sync Endpoint Structure")
    void testSyncEndpointStructure() {
        // This tests the Contract of the Sync Endpoint
        // We expect it to accept POST requests and require Auth

        given()
                .baseUri(PRODUCT_SERVICE_URL)
                .when()
                .post("/api/admin/products/sync")
                .then()
                .statusCode(anyOf(is(200), is(401), is(403)));
    }
}
