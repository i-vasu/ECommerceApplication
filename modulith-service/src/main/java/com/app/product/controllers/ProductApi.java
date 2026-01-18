package com.app.product.controllers;

import com.app.product.entites.Product;
import com.app.product.payloads.ProductDTO;
import com.app.product.payloads.ProductResponse;
import com.app.review.payloads.ProductReviewDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Tag(name = "Product", description = "Product Management and Catalog APIs")
public interface ProductApi {

        @Operation(summary = "Get Product by ID", description = "Retrieves a specific product by its ID")
        ResponseEntity<ProductDTO> getProductById(Long productId);

        @Operation(summary = "Get Products by IDs", description = "Retrieves a batch of products by their IDs")
        ResponseEntity<List<ProductDTO>> getProductsByIds(List<Long> productIds);

        @Operation(summary = "Get Products by Category", description = "Retrieves products belonging to a specific category")
        ResponseEntity<ProductResponse> getProductsByCategory(Long categoryId, Integer pageNumber, Integer pageSize,
                        String sortBy, String sortOrder);

        @Operation(summary = "Search Products", description = "Search products by keyword")
        ResponseEntity<ProductResponse> searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize,
                        String sortBy, String sortOrder);

        @Operation(summary = "Update Product", description = "Updates detailed product information (Admin only)")
        ResponseEntity<ProductDTO> updateProduct(Product product, Long productId);

        @Operation(summary = "Update Product Image", description = "Updates the main image of a product (Admin only)")
        ResponseEntity<ProductDTO> updateProductImage(Long productId, MultipartFile image) throws IOException;

        @Operation(summary = "Add Product Media", description = "Adds additional media (images/videos) to a product (Admin only)")
        ResponseEntity<ProductDTO> addMedia(Long productId, MultipartFile file, String type) throws IOException;

        @Operation(summary = "Add Product Review", description = "Adds a user review for a product")
        ResponseEntity<ProductDTO> addReview(Long productId, ProductReviewDTO reviewDTO);

        @Operation(summary = "Delete Product", description = "Deletes a product (Admin only)")
        ResponseEntity<String> deleteProduct(Long productId);
}
