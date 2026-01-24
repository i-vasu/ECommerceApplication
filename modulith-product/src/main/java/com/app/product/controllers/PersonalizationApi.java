package com.app.product.controllers;

import com.app.product.payloads.ProductDTO;
import com.app.core.payloads.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "Personalization v1", description = "User Personalization APIs - Version 1")
public interface PersonalizationApi {

    @Operation(summary = "Get Recently Viewed Products")
    ResponseEntity<ApiResponse<List<ProductDTO>>> getRecentlyViewed(Long userId, int limit);

    @Operation(summary = "Get Newly Dropped Products")
    ResponseEntity<ApiResponse<List<ProductDTO>>> getNewlyDropped(int limit);

    @Operation(summary = "Get Trending Products")
    ResponseEntity<ApiResponse<List<ProductDTO>>> getTrending(int limit);

    @Operation(summary = "Get Best Selling Products")
    ResponseEntity<ApiResponse<List<ProductDTO>>> getBestSellers(int limit);

    @Operation(summary = "Track Product View")
    ResponseEntity<ApiResponse<Void>> trackView(Long userId, Long productId);

    @Operation(summary = "Get Search History")
    ResponseEntity<ApiResponse<List<String>>> getSearchHistory(Long userId, int limit);
}
