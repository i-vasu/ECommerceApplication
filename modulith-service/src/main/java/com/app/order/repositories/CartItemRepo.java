package com.app.order.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.app.order.entites.CartItem;

public interface CartItemRepo extends JpaRepository<CartItem, Long> {

	@Query("SELECT ci FROM CartItem ci WHERE ci.cart.cartId = ?1 AND ci.productId = ?2")
	CartItem findCartItemByProductIdAndCartId(Long cartId, Long productId);

	@Modifying
	@Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = ?1 AND ci.productId = ?2")
	void deleteCartItemByProductIdAndCartId(Long cartId, Long productId);
}
