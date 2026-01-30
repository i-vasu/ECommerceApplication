package com.app.cart.repositories;

import com.app.cart.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import com.app.governance.states.CartState;

public interface CartRepo extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserEmail(String email);

    @Query("SELECT c FROM Cart c WHERE c.user.email = ?1 AND c.cartId = ?2")
    Cart findCartByEmailAndCartId(String email, Long cartId);

    @Query("SELECT c FROM Cart c WHERE c.lastUpdated < ?1 AND size(c.cartItems) > 0")
    List<Cart> findAbandonedCarts(LocalDateTime cutoff);

    List<Cart> findByStatusAndLastUpdatedBefore(CartState status,
            LocalDateTime cutoff);
}
