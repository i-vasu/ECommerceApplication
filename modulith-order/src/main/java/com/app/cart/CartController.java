package com.app.cart;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import com.app.cart.payloads.CartDTO;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class CartController implements CartApi {

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

	@GetMapping("/public/users/{emailId}/carts/{cartId}")
	@Override
	public ResponseEntity<ApiResponse<CartDTO>> getCartById(@PathVariable String emailId, @PathVariable Long cartId) {
		var cartDTO = cartService.getCart(emailId, cartId);
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
}
