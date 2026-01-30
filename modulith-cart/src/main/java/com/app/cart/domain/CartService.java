package com.app.cart.domain;

import com.app.cart.payloads.CartDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CartService {
	CartDTO createCart();

	CartDTO addProductToCart(Long cartId, Long productId, String itemCode, Integer quantity);

	Page<CartDTO> getAllCarts(Pageable pageable);

	CartDTO getCart(String emailId, Long cartId);

	CartDTO updateProductQuantityInCart(Long cartId, Long productId, String itemCode, Integer quantity);

	void updateProductInCarts(Long cartId, Long productId);

	String deleteProductFromCart(Long cartId, Long productId);

	CartDTO applyCoupon(Long cartId, String couponCode);

	CartDTO removeCoupon(Long cartId);
}
