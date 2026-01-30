package com.app.catalog.repositories;

import com.app.catalog.entities.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepo extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUserEmail(String email);

    Optional<Wishlist> findByUserId(Long userId);
}
