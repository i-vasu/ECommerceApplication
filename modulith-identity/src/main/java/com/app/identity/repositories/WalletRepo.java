package com.app.identity.repositories;

import com.app.identity.entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepo extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserEmail(String email);

    Optional<Wallet> findByUserId(Long userId);
}
