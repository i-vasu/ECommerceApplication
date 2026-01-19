package com.app.cart;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.order.payloads.CartDTO;
import com.app.cart.CartService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "E-Commerce Application")
public class CartController implements CartApi {

	@Autowired
	private CartService cartService;

	@PostMapping("/public/carts/{cartId}/products/{productId}/quantity/{quantity}")
	@Override
	public ResponseEntity<CartDTO> addProductToCart(@PathVariable Long cartId,
			@PathVariable Long productId,
			@PathVariable Integer quantity,
			@RequestParam(required = false) String itemCode) {
		CartDTO cartDTO = cartService.addProductToCart(cartId, productId, itemCode, quantity);
		return new ResponseEntity<CartDTO>(cartDTO, HttpStatus.CREATED);
	}

	@GetMapping("/admin/carts")
	@Override
	public ResponseEntity<List<CartDTO>> getCarts() {
		List<CartDTO> cartDTOs = cartService.getAllCarts();
		return new ResponseEntity<List<CartDTO>>(cartDTOs, HttpStatus.FOUND);
	}

	@GetMapping("/public/users/{emailId}/carts/{cartId}")
	@Override
	public ResponseEntity<CartDTO> getCartById(@PathVariable String emailId, @PathVariable Long cartId) {
		CartDTO cartDTO = cartService.getCart(emailId, cartId);
		return new ResponseEntity<CartDTO>(cartDTO, HttpStatus.FOUND);
	}

	@PutMapping("/public/carts/{cartId}/products/{productId}/quantity/{quantity}")
	@Override
	public ResponseEntity<CartDTO> updateCartProduct(@PathVariable Long cartId,
			@PathVariable Long productId,
			@PathVariable Integer quantity,
			@RequestParam(required = false) String itemCode) {
		CartDTO cartDTO = cartService.updateProductQuantityInCart(cartId, productId, itemCode, quantity);
		return new ResponseEntity<CartDTO>(cartDTO, HttpStatus.OK);
	}

	@DeleteMapping("/public/carts/{cartId}/product/{productId}")
	@Override
	public ResponseEntity<String> deleteProductFromCart(@PathVariable Long cartId, @PathVariable Long productId) {
		String status = cartService.deleteProductFromCart(cartId, productId);
		return new ResponseEntity<String>(status, HttpStatus.OK);
	}
}
