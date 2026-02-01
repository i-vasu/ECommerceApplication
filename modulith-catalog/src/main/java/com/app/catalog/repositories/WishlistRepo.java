package com.app.catalog.repositories;

import com.app.catalog.entities.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepo extends JpaRepository<Wishlist, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT w FROM Wishlist w WHERE w.userId = :userId")
    Optional<Wishlist> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
