package com.app.identity.controllers;

import com.app.identity.services.PersonalizationService;
import com.app.product.payloads.ProductDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personalization")
public class PersonalizationController {

    @Autowired
    private PersonalizationService personalizationService;

    @GetMapping("/recently-viewed")
    public ResponseEntity<List<ProductDTO>> getRecentlyViewed(@RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        var items = personalizationService.getRecentlyViewed(userId, limit);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/newly-dropped")
    public ResponseEntity<List<ProductDTO>> getNewlyDropped(@RequestParam(defaultValue = "10") int limit) {
        var items = personalizationService.getNewlyDropped(limit);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<ProductDTO>> getTrending(@RequestParam(defaultValue = "10") int limit) {
        var items = personalizationService.getTrending(limit);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/best-sellers")
    public ResponseEntity<List<ProductDTO>> getBestSellers(@RequestParam(defaultValue = "10") int limit) {
        var items = personalizationService.getBestSellers(limit);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/track-view/{productId}")
    public ResponseEntity<Void> trackView(@RequestParam Long userId, @PathVariable Long productId) {
        personalizationService.trackProductView(userId, productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search-history")
    public ResponseEntity<List<String>> getSearchHistory(@RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        var history = personalizationService.getSearchHistory(userId, limit);
        return ResponseEntity.ok(history);
    }
}
