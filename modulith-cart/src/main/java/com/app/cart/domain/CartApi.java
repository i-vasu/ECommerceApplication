package com.app.cart.domain;

import com.app.cart.payloads.CartDTO;
import com.app.core.payloads.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Cart v1", description = "Shopping Cart Management APIs - Version 1")
public interface CartApi {
        @Operation(summary = "Create Empty Cart", description = "Initializes a new shopping cart")
        ResponseEntity<ApiResponse<CartDTO>> createCart();

        @Operation(summary = "Add Product to Cart", description = "Adds a product to the cart")
        ResponseEntity<ApiResponse<CartDTO>> addProductToCart(Long cartId, Long productId, Integer quantity,
                        String itemCode);

        @Operation(summary = "Get All Carts", description = "Retrieves all carts (Admin only)")
        ResponseEntity<ApiResponse<Page<CartDTO>>> getCarts(Pageable pageable);

        @Operation(summary = "Get Cart by ID", description = "Retrieves cart details by ID")
        ResponseEntity<ApiResponse<CartDTO>> getCartById(Long userId, Long cartId);

        @Operation(summary = "Update Cart Product", description = "Updates the quantity of a product in the cart")
        ResponseEntity<ApiResponse<CartDTO>> updateCartProduct(Long cartId, Long productId, Integer quantity,
                        String itemCode);

        @Operation(summary = "Delete Product from Cart", description = "Removes a product from the cart")
        ResponseEntity<ApiResponse<String>> deleteProductFromCart(Long cartId, Long productId);

        @Operation(summary = "Update Cart Address", description = "Sets the address for the cart and recalculates totals")
        ResponseEntity<ApiResponse<CartDTO>> updateCartAddress(Long cartId, Long addressId);

        @Operation(summary = "Merge Guest Cart", description = "Merges a guest cart into a user's cart")
        ResponseEntity<ApiResponse<CartDTO>> mergeCarts(Long guestCartId, Long userId);
}
