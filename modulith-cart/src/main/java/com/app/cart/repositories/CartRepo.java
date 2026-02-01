package com.app.cart.repositories;

import com.app.cart.entities.Cart;
import com.app.governance.states.CartState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CartRepo extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    @Query("SELECT c FROM Cart c WHERE c.userId = ?1 AND c.cartId = ?2")
    Cart findCartByUserIdAndCartId(Long userId, Long cartId);

    @Query("SELECT c FROM Cart c WHERE c.lastUpdated < ?1 AND size(c.cartItems) > 0")
    List<Cart> findAbandonedCarts(LocalDateTime cutoff);

    List<Cart> findByStatusAndLastUpdatedBefore(CartState status,
            LocalDateTime cutoff);
}
