package com.app.cart.domain;

import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import com.app.cart.entities.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/carts")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
public class RedisCartController implements RedisCartApi {

    @Autowired
    private RedisCartService cartService;

    @PostMapping("/{userId}/add")
    @Override
    public ResponseEntity<ApiResponse<String>> addToCart(@PathVariable String userId, @RequestBody CartItem item) {
        cartService.addToCart(userId, item);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", "Success"));
    }

    @DeleteMapping("/{userId}/remove/{productId}")
    @Override
    public ResponseEntity<ApiResponse<String>> removeFromCart(@PathVariable String userId,
            @PathVariable String productId) {
        cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", "Success"));
    }

    @GetMapping("/{userId}")
    @Override
    public ResponseEntity<ApiResponse<List<CartItem>>> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(userId), "Cart retrieved successfully"));
    }

    @DeleteMapping("/{userId}/clear")
    @Override
    public ResponseEntity<ApiResponse<String>> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", "Success"));
    }
}
