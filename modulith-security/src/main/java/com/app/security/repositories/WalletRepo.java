package com.app.security.repositories;

import com.app.security.entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepo extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM Wallet w WHERE w.user.userId = :userId")
    Optional<Wallet> findByUserId(Long userId);
}
