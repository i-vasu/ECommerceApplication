package com.app.cart;

import com.app.payloads.CartItem;
import com.app.services.RedisCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart/redis")
public class RedisCartController {

    @Autowired
    private RedisCartService cartService;

    @PostMapping("/{userId}/add")
    public ResponseEntity<String> addToCart(@PathVariable String userId, @RequestBody CartItem item) {
        cartService.addToCart(userId, item);
        return ResponseEntity.ok("Item added to cart");
    }

    @DeleteMapping("/{userId}/remove/{productId}")
    public ResponseEntity<String> removeFromCart(@PathVariable String userId, @PathVariable String productId) {
        cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok("Item removed from cart");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<CartItem>> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<String> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}
