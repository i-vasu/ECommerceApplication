package com.app.cart.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.app.cart.entities.CartItem;

public interface CartItemRepo extends JpaRepository<CartItem, Long> {

	@Query("SELECT ci FROM CartItem ci WHERE ci.cart.cartId = ?1 AND ci.productId = ?2 AND ci.itemCode = ?3")
	CartItem findCartItemByProductIdAndCartIdAndItemCode(Long cartId, Long productId, String itemCode);

	@Query("SELECT ci FROM CartItem ci WHERE ci.cart.cartId = ?1 AND ci.productId = ?2")
	CartItem findCartItemByProductIdAndCartId(Long cartId, Long productId);

	@Modifying
	@Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = ?1 AND ci.productId = ?2")
	void deleteCartItemByProductIdAndCartId(Long cartId, Long productId);
}
