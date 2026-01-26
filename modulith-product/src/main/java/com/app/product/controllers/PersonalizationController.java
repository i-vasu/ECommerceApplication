package com.app.product.controllers;

import com.app.product.services.PersonalizationService;
import com.app.product.payloads.ProductDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/personalization")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
public class PersonalizationController implements PersonalizationApi {

    @Autowired
    private PersonalizationService personalizationService;

    @GetMapping("/recently-viewed")
    @Override
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getRecentlyViewed(@RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        var items = personalizationService.getRecentlyViewed(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(items, "Recently viewed retrieved successfully"));
    }

    @GetMapping("/newly-dropped")
    @Override
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getNewlyDropped(@RequestParam(defaultValue = "10") int limit) {
        var items = personalizationService.getNewlyDropped(limit);
        return ResponseEntity.ok(ApiResponse.success(items, "Newly dropped retrieved successfully"));
    }

    @GetMapping("/trending")
    @Override
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getTrending(@RequestParam(defaultValue = "10") int limit) {
        var items = personalizationService.getTrending(limit);
        return ResponseEntity.ok(ApiResponse.success(items, "Trending retrieved successfully"));
    }

    @GetMapping("/best-sellers")
    @Override
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getBestSellers(@RequestParam(defaultValue = "10") int limit) {
        var items = personalizationService.getBestSellers(limit);
        return ResponseEntity.ok(ApiResponse.success(items, "Best sellers retrieved successfully"));
    }

    @PostMapping("/track-view/{productId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> trackView(@RequestParam Long userId, @PathVariable Long productId) {
        personalizationService.trackProductView(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(null, "View tracked successfully"));
    }

    @GetMapping("/search-history")
    @Override
    public ResponseEntity<ApiResponse<List<String>>> getSearchHistory(@RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        var history = personalizationService.getSearchHistory(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(history, "Search history retrieved successfully"));
    }
}
