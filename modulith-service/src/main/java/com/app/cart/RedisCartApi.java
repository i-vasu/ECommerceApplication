package com.app.cart;

import com.app.order.entities.CartItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "Redis Cart", description = "High-Performance Redis Cart APIs")
public interface RedisCartApi {

    @Operation(summary = "Add Item to Redis Cart", description = "Adds an item to the Redis cart")
    ResponseEntity<String> addToCart(String userId, CartItem item);

    @Operation(summary = "Remove Item from Redis Cart", description = "Removes an item from the Redis cart")
    ResponseEntity<String> removeFromCart(String userId, String productId);

    @Operation(summary = "Get Redis Cart", description = "Retrieves the Redis cart for a user")
    ResponseEntity<List<CartItem>> getCart(String userId);

    @Operation(summary = "Clear Redis Cart", description = "Clears the Redis cart for a user")
    ResponseEntity<String> clearCart(String userId);
}
