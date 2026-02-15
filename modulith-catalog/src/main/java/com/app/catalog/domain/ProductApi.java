package com.app.catalog.domain;

import com.app.catalog.entities.Product;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.payloads.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

@Tag(name = "Product v1", description = "Public Catalog and Admin Product Management APIs")
public interface ProductApi {

    @Operation(summary = "Create Product", description = "Adds a new product to a specific category. Restricted to ADMIN.")
    @ApiResponse(responseCode = "201", description = "Product created")
    ResponseEntity<com.app.core.payloads.ApiResponse<ProductDTO>> createProduct(ProductDTO productDTO, Long categoryId);

    @Operation(summary = "List All Products", description = "Retrieves a paginated list of all products in the catalog.")
    @ApiResponse(responseCode = "200", description = "Success")
    ResponseEntity<com.app.core.payloads.ApiResponse<ProductResponse>> getProducts(
            Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    @Operation(summary = "Products by Category", description = "Retrieves products belonging to a specific category.")
    ResponseEntity<com.app.core.payloads.ApiResponse<ProductResponse>> getProductsByCategory(
            Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    @Operation(summary = "Search by Keyword", description = "Full-text search for products by name or description.")
    ResponseEntity<com.app.core.payloads.ApiResponse<ProductResponse>> getProductsByKeyword(
            String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    @Operation(summary = "Faceted Filter Search", description = "Advanced search with keyword and price range filtering.")
    ResponseEntity<com.app.core.payloads.ApiResponse<ProductResponse>> facetedSearch(
            String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    @Operation(summary = "Product Details", description = "Retrieves details of a product by its ID or item code (slug).")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    ResponseEntity<com.app.core.payloads.ApiResponse<ProductDTO>> getProductById(
            @Parameter(description = "Numeric ID or String Item Code/Slug") String productId);

    @Operation(summary = "Update Product", description = "Modifies an existing product. Restricted to ADMIN.")
    ResponseEntity<com.app.core.payloads.ApiResponse<ProductDTO>> updateProduct(ProductDTO productDTO, Long productId);

    @Operation(summary = "Delete Product", description = "Permanently removes a product from the catalog. Restricted to ADMIN.")
    ResponseEntity<com.app.core.payloads.ApiResponse<String>> deleteProduct(Long productId);
}
