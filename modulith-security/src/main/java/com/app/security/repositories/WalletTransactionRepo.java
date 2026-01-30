package com.app.security.repositories;

import com.app.security.entities.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepo extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletWalletId(Long walletId);
}
