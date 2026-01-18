package com.app.cart;

import com.app.order.entites.CartItem;
import com.app.cart.RedisCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/carts")
@SecurityRequirement(name = "E-Commerce Application")
public class RedisCartController implements RedisCartApi {

    @Autowired
    private RedisCartService cartService;

    @PostMapping("/{userId}/add")
    @Override
    public ResponseEntity<String> addToCart(@PathVariable String userId, @RequestBody CartItem item) {
        cartService.addToCart(userId, item);
        return ResponseEntity.ok("Item added to cart");
    }

    @DeleteMapping("/{userId}/remove/{productId}")
    @Override
    public ResponseEntity<String> removeFromCart(@PathVariable String userId, @PathVariable String productId) {
        cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok("Item removed from cart");
    }

    @GetMapping("/{userId}")
    @Override
    public ResponseEntity<List<CartItem>> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @DeleteMapping("/{userId}/clear")
    @Override
    public ResponseEntity<String> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}
