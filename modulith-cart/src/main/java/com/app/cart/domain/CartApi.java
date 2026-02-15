package com.app.cart.domain;

import com.app.cart.payloads.CartDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Cart v1", description = "Shopping Cart Management APIs - Version 1")
public interface CartApi {
        @Operation(summary = "Create Empty Cart", description = "Initializes a new shopping cart for a guest or user.")
        @ApiResponse(responseCode = "201", description = "Cart created successfully")
        ResponseEntity<com.app.core.payloads.ApiResponse<CartDTO>> createCart();

        @Operation(summary = "Add Product to Cart", description = "Adds a specific quantity of a product to a cart.")
        @ApiResponse(responseCode = "201", description = "Product added successfully")
        @ApiResponse(responseCode = "404", description = "Cart or Product not found")
        ResponseEntity<com.app.core.payloads.ApiResponse<CartDTO>> addProductToCart(
                        @Parameter(description = "ID of the cart") Long cartId,
                        @Parameter(description = "ID of the product") Long productId,
                        @Parameter(description = "Quantity to add") Integer quantity,
                        @Parameter(description = "Optional variant code") String itemCode);

        @Operation(summary = "Get All Carts", description = "Retrieves a paginated list of all active carts. Restricted to Admins.")
        @ApiResponse(responseCode = "200", description = "Carts retrieved successfully")
        ResponseEntity<com.app.core.payloads.ApiResponse<Page<CartDTO>>> getCarts(Pageable pageable);

        @Operation(summary = "Get User Cart", description = "Retrieves cart details for a specific user and cart ID.")
        @ApiResponse(responseCode = "200", description = "Cart retrieved successfully")
        ResponseEntity<com.app.core.payloads.ApiResponse<CartDTO>> getCartById(
                        @Parameter(description = "ID of the user") Long userId,
                        @Parameter(description = "ID of the cart") Long cartId);

        @Operation(summary = "Update Product Quantity", description = "Updates the quantity of an existing item in the cart.")
        @ApiResponse(responseCode = "200", description = "Cart updated successfully")
        ResponseEntity<com.app.core.payloads.ApiResponse<CartDTO>> updateCartProduct(
                        @Parameter(description = "ID of the cart") Long cartId,
                        @Parameter(description = "ID of the product") Long productId,
                        @Parameter(description = "New quantity") Integer quantity,
                        @Parameter(description = "Optional variant code") String itemCode);

        @Operation(summary = "Remove Product", description = "Removes a specific product from the cart.")
        @ApiResponse(responseCode = "200", description = "Product removed successfully")
        ResponseEntity<com.app.core.payloads.ApiResponse<String>> deleteProductFromCart(
                        @Parameter(description = "ID of the cart") Long cartId,
                        @Parameter(description = "ID of the product") Long productId);

        @Operation(summary = "Apply Coupon", description = "Validates and applies a coupon code to the cart.")
        @ApiResponse(responseCode = "200", description = "Coupon applied successfully")
        @ApiResponse(responseCode = "400", description = "Invalid or expired coupon")
        ResponseEntity<com.app.core.payloads.ApiResponse<CartDTO>> applyCoupon(
                        @Parameter(description = "ID of the cart") Long cartId,
                        @Parameter(description = "Coupon code string") String couponCode);

        @Operation(summary = "Update Shipping Address", description = "Sets the shipping address for the cart and recalculates taxes/shipping costs.")
        @ApiResponse(responseCode = "200", description = "Address updated successfully")
        ResponseEntity<com.app.core.payloads.ApiResponse<CartDTO>> updateCartAddress(
                        @Parameter(description = "ID of the cart") Long cartId,
                        @Parameter(description = "ID of the address") Long addressId);

        @Operation(summary = "Merge Guest Cart", description = "Merges items from a guest cart into a registered user's cart upon login.")
        @ApiResponse(responseCode = "200", description = "Carts merged successfully")
        ResponseEntity<com.app.core.payloads.ApiResponse<CartDTO>> mergeCarts(
                        @Parameter(description = "ID of the guest cart") Long guestCartId,
                        @Parameter(description = "ID of the target user") Long userId);

        @Operation(summary = "Clear Cart", description = "Removes all items from the specified cart.")
        @ApiResponse(responseCode = "200", description = "Cart cleared successfully")
        ResponseEntity<com.app.core.payloads.ApiResponse<String>> clearCart(
                        @Parameter(description = "ID of the cart to clear") Long cartId);
}
