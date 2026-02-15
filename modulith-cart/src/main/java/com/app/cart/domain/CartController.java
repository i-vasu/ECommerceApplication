package com.app.cart.domain;

import com.app.cart.payloads.CartDTO;
import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class CartController implements CartApi {
	@PostMapping("/public/carts")
	@Override
	public ResponseEntity<ApiResponse<CartDTO>> createCart() {
		var cartDTO = cartService.createCart();
		return new ResponseEntity<>(ApiResponse.success(cartDTO, "Cart initialized"), HttpStatus.CREATED);
	}

	private final CartService cartService;

	@PostMapping("/public/carts/{cartId}/products/{productId}/quantity/{quantity}")
	@Override
	public ResponseEntity<ApiResponse<CartDTO>> addProductToCart(@PathVariable Long cartId,
			@PathVariable Long productId,
			@PathVariable Integer quantity,
			@RequestParam(required = false) String itemCode) {
		var cartDTO = cartService.addProductToCart(cartId, productId, itemCode, quantity);
		return new ResponseEntity<>(ApiResponse.success(cartDTO, "Product added to cart"), HttpStatus.CREATED);
	}

	@GetMapping("/admin/carts")
	@Override
	public ResponseEntity<ApiResponse<Page<CartDTO>>> getCarts(Pageable pageable) {
		var cartDTOs = cartService.getAllCarts(pageable);
		return new ResponseEntity<>(ApiResponse.success(cartDTOs, "Carts retrieved successfully"), HttpStatus.OK);
	}

	@GetMapping("/public/users/{userId}/carts/{cartId}")
	@Override
	public ResponseEntity<ApiResponse<CartDTO>> getCartById(@PathVariable Long userId, @PathVariable Long cartId) {
		var cartDTO = cartService.getCart(userId, cartId);
		return new ResponseEntity<>(ApiResponse.success(cartDTO, "Cart retrieved successfully"), HttpStatus.OK);
	}

	@PutMapping("/public/carts/{cartId}/products/{productId}/quantity/{quantity}")
	@Override
	public ResponseEntity<ApiResponse<CartDTO>> updateCartProduct(@PathVariable Long cartId,
			@PathVariable Long productId,
			@PathVariable Integer quantity,
			@RequestParam(required = false) String itemCode) {
		var cartDTO = cartService.updateProductQuantityInCart(cartId, productId, itemCode, quantity);
		return new ResponseEntity<>(ApiResponse.success(cartDTO, "Cart updated successfully"), HttpStatus.OK);
	}

	@DeleteMapping("/public/carts/{cartId}/product/{productId}")
	@Override
	public ResponseEntity<ApiResponse<String>> deleteProductFromCart(@PathVariable Long cartId,
			@PathVariable Long productId) {
		var status = cartService.deleteProductFromCart(cartId, productId);
		return new ResponseEntity<>(ApiResponse.success(status, "Product removed from cart"), HttpStatus.OK);
	}

	@PostMapping("/public/carts/{cartId}/coupon/{couponCode}")
	public ResponseEntity<ApiResponse<CartDTO>> applyCoupon(@PathVariable Long cartId,
			@PathVariable String couponCode) {
		var cartDTO = cartService.applyCoupon(cartId, couponCode);
		return ResponseEntity.ok(ApiResponse.success(cartDTO, "Coupon applied successfully"));
	}

	@Override
	@PutMapping("/public/carts/{cartId}/address/{addressId}")
	public ResponseEntity<ApiResponse<CartDTO>> updateCartAddress(@PathVariable Long cartId,
			@PathVariable Long addressId) {
		var cartDTO = cartService.updateCartAddress(cartId, addressId);
		return ResponseEntity.ok(ApiResponse.success(cartDTO, "Cart address updated and totals recalculated"));
	}

	@Override
	@PostMapping("/public/carts/{guestCartId}/merge/user/{userId}")
	public ResponseEntity<ApiResponse<CartDTO>> mergeCarts(@PathVariable Long guestCartId, @PathVariable Long userId) {
		var cartDTO = cartService.mergeCarts(guestCartId, userId);
		return ResponseEntity.ok(ApiResponse.success(cartDTO, "Guest cart merged successfully"));
	}

	@DeleteMapping("/public/carts/{cartId}/clear")
	public ResponseEntity<ApiResponse<String>> clearCart(@PathVariable Long cartId) {
		cartService.clearCart(cartId);
		return ResponseEntity.ok(ApiResponse.success("Cart cleared", "All items removed from cart"));
	}
}
