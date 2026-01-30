package com.app.test.integration;

import com.app.security.entities.Wallet;
import com.app.security.repositories.WalletRepo;
import com.app.security.services.WalletService;
import com.app.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class WalletIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepo walletRepo;

    @Test
    void testWalletFinancialFlow() {
        String email = "wallet_user@example.com";
        
        // 1. Get/Create Wallet
        Wallet wallet = walletService.getOrCreateWallet(email);
        assertThat(wallet.getBalance()).isEqualTo(0.0);
        
        // 2. Credit Refund
        walletService.credit(email, 1000.0, "Refund for Order #1", "ORD1");
        assertThat(walletService.getBalance(email)).isEqualTo(1000.0);
        
        // 3. Debit Purchase
        walletService.debit(email, 300.0, "Purchase #2", "ORD2");
        assertThat(walletService.getBalance(email)).isEqualTo(700.0);
        
        // 4. Verify Transactions
        Wallet updatedWallet = walletRepo.findByEmail(email).orElseThrow();
        assertThat(updatedWallet.getTransactions()).hasSize(2);
        assertThat(updatedWallet.getTransactions().get(0).getAmount()).isNotNull();
    }
}
