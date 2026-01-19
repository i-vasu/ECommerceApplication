package com.app.cart;

import com.app.order.payloads.CartDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "Cart", description = "Shopping Cart Management APIs")
public interface CartApi {

    @Operation(summary = "Add Product to Cart", description = "Adds a product to the cart")
    ResponseEntity<CartDTO> addProductToCart(Long cartId, Long productId, Integer quantity, String itemCode);

    @Operation(summary = "Get All Carts", description = "Retrieves all carts (Admin only)")
    ResponseEntity<List<CartDTO>> getCarts();

    @Operation(summary = "Get Cart by ID", description = "Retrieves cart details by ID")
    ResponseEntity<CartDTO> getCartById(String emailId, Long cartId);

    @Operation(summary = "Update Cart Product", description = "Updates the quantity of a product in the cart")
    ResponseEntity<CartDTO> updateCartProduct(Long cartId, Long productId, Integer quantity, String itemCode);

    @Operation(summary = "Delete Product from Cart", description = "Removes a product from the cart")
    ResponseEntity<String> deleteProductFromCart(Long cartId, Long productId);
}
