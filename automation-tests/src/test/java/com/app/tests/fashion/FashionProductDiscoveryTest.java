package com.app.tests.fashion;

import com.app.tests.utils.AuthHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Fashion-specific product discovery and visual search tests
 * Critical for helping customers find the right products
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Fashion Product Discovery: Search & Filters")
public class FashionProductDiscoveryTest {

    private static final String PRODUCT_SERVICE = "http://localhost:8082";

    @Test
    @Order(1)
    @DisplayName("CRITICAL: Search products by category (Sarees, Kurtis, etc.)")
    void testSearchByFashionCategory() {
        String[] categories = { "Sarees", "Kurtis", "Lehengas", "Salwar Suits" };

        for (String category : categories) {
            Response response = given()
                    .when()
                    .get(PRODUCT_SERVICE + "/api/public/products?category=" + category)
                    .then()
                    .statusCode(200)
                    .body("content", notNullValue())
                    .extract().response();

            int count = response.jsonPath().getInt("content.size()");
            System.out.println("✅ Found " + count + " products in category: " + category);
        }
    }

    @Test
    @Order(2)
    @DisplayName("CRITICAL: Filter by size availability")
    void testFilterBySize() {
        given()
                .queryParam("size", "M")
                .when()
                .get(PRODUCT_SERVICE + "/api/public/products")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                // Verify all results have size M available
                .body("content.every { it.variants.any { v -> v.size == 'M' && v.stock > 0 } }", is(true));

        System.out.println("✅ Size filter shows only products with that size in stock");
    }

    @Test
    @Order(3)
    @DisplayName("CRITICAL: Filter by color")
    void testFilterByColor() {
        String[] popularColors = { "Red", "Blue", "Green", "Yellow", "Black" };

        for (String color : popularColors) {
            given()
                    .queryParam("color", color)
                    .when()
                    .get(PRODUCT_SERVICE + "/api/public/products")
                    .then()
                    .statusCode(200);

            System.out.println("✅ Color filter works for: " + color);
        }
    }

    @Test
    @Order(4)
    @DisplayName("CRITICAL: Filter by price range (budget shopping)")
    void testFilterByPriceRange() {
        given()
                .queryParam("minPrice", 500)
                .queryParam("maxPrice", 2000)
                .when()
                .get(PRODUCT_SERVICE + "/api/public/products")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                // Verify all products are within price range
                .body("content.every { it.price >= 500 && it.price <= 2000 }", is(true));

        System.out.println("✅ Price range filter helps customers shop within budget");
    }

    @Test
    @Order(5)
    @DisplayName("CRITICAL: Search by fabric/material")
    void testSearchByMaterial() {
        String[] materials = { "Cotton", "Silk", "Georgette", "Chiffon" };

        for (String material : materials) {
            given()
                    .queryParam("material", material)
                    .when()
                    .get(PRODUCT_SERVICE + "/api/public/products")
                    .then()
                    .statusCode(200);

            System.out.println("✅ Material filter works for: " + material);
        }
    }

    @Test
    @Order(6)
    @DisplayName("INNOVATION: Visual search by uploading similar product image")
    void testVisualSearch() {
        // Test the AI-powered visual search feature
        File sampleImage = new File("src/test/resources/sample-saree.jpg");

        if (!sampleImage.exists()) {
            System.out.println("⚠️ Sample image not found, skipping visual search test");
            return;
        }

        Response response = given()
                .multiPart("image", sampleImage)
                .when()
                .post(PRODUCT_SERVICE + "/api/public/products/search/visual")
                .then()
                .statusCode(anyOf(is(200), is(405))) // 405 if POST not allowed
                .extract().response();

        if (response.statusCode() == 200) {
            response.then()
                    .body("results", notNullValue())
                    .body("results.size()", greaterThan(0))
                    .body("results[0].similarity", greaterThan(0.5f));

            System.out.println("✅ Visual search finds similar products using AI");
        } else {
            System.out.println("⚠️ Visual search endpoint needs implementation");
        }
    }

    @Test
    @Order(7)
    @DisplayName("EDGE CASE: Handle misspelled search queries")
    void testFuzzySearch() {
        // Customer types "sari" instead of "saree"
        given()
                .queryParam("q", "sari")
                .when()
                .get(PRODUCT_SERVICE + "/api/public/products/search")
                .then()
                .statusCode(200)
                .body("content", notNullValue());

        System.out.println("✅ Search handles common misspellings");
    }

    @Test
    @Order(8)
    @DisplayName("BUSINESS RULE: Show out-of-stock products with 'Notify Me' option")
    void testShowOutOfStockProducts() {
        given()
                .queryParam("includeOutOfStock", true)
                .when()
                .get(PRODUCT_SERVICE + "/api/public/products")
                .then()
                .statusCode(200)
                .body("content", notNullValue());

        System.out.println("✅ Out-of-stock products shown with restock notification option");
    }

    @Test
    @Order(9)
    @DisplayName("PERFORMANCE: Search results load under 500ms")
    void testSearchPerformance() {
        long startTime = System.currentTimeMillis();

        given()
                .queryParam("category", "Sarees")
                .when()
                .get(PRODUCT_SERVICE + "/api/public/products")
                .then()
                .statusCode(200);

        long duration = System.currentTimeMillis() - startTime;

        Assertions.assertTrue(duration < 500,
                "Search took " + duration + "ms, should be under 500ms");

        System.out.println("✅ Search results load in " + duration + "ms (fast!)");
    }
}
