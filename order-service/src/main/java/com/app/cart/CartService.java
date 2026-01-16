package com.app.cart;

import java.util.List;

import com.app.payloads.CartDTO;

public interface CartService {

	CartDTO addProductToCart(Long cartId, Long productId, String itemCode, Integer quantity);

	List<CartDTO> getAllCarts();

	CartDTO getCart(String emailId, Long cartId);

	CartDTO updateProductQuantityInCart(Long cartId, Long productId, String itemCode, Integer quantity);

	void updateProductInCarts(Long cartId, Long productId);

	String deleteProductFromCart(Long cartId, Long productId);

}
