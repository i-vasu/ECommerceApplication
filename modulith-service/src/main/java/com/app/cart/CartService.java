package com.app.cart;

import java.util.List;

import com.app.order.payloads.CartDTO;

public interface CartService {

	CartDTO addProductToCart(Long cartId, Long productId, String itemCode, Integer quantity);

	org.springframework.data.domain.Page<CartDTO> getAllCarts(org.springframework.data.domain.Pageable pageable);

	CartDTO getCart(String emailId, Long cartId);

	CartDTO updateProductQuantityInCart(Long cartId, Long productId, String itemCode, Integer quantity);

	void updateProductInCarts(Long cartId, Long productId);

	String deleteProductFromCart(Long cartId, Long productId);

}
