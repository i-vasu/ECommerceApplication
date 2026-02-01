package com.app.security.services;

import com.app.core.APIException;
import com.app.security.entities.Wallet;
import com.app.security.entities.WalletTransaction;
import com.app.security.repositories.WalletRepo;
import com.app.security.repositories.WalletTransactionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepo walletRepo;
    @Mock
    private WalletTransactionRepo transactionRepo;

    @InjectMocks
    private WalletService walletService;

    private Wallet testWallet;
    private String userEmail = "wallet@example.com";

    @BeforeEach
    void setUp() {
        testWallet = new Wallet();
        com.app.security.entities.User user = new com.app.security.entities.User();
        user.setEmail(userEmail);
        testWallet.setUser(user);
        testWallet.setBalance(500.0);
    }

    @Test
    void testGetOrCreateWallet_Existing() {
        when(walletRepo.findByUserEmail(userEmail)).thenReturn(Optional.of(testWallet));
        
        Wallet wallet = walletService.getOrCreateWallet(userEmail);
        
        assertEquals(500.0, wallet.getBalance());
        verify(walletRepo, never()).save(any());
    }

    @Test
    void testGetOrCreateWallet_New() {
        when(walletRepo.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(walletRepo.save(any(Wallet.class))).thenAnswer(i -> i.getArguments()[0]);
        
        Wallet wallet = walletService.getOrCreateWallet(userEmail);
        
        assertEquals(0.0, wallet.getBalance());
        verify(walletRepo).save(any());
    }

    @Test
    void testCredit() {
        when(walletRepo.findByUserEmail(userEmail)).thenReturn(Optional.of(testWallet));
        
        walletService.credit(userEmail, 200.0, "Refund", "REF123");
        
        assertEquals(700.0, testWallet.getBalance());
        verify(transactionRepo).save(any(WalletTransaction.class));
    }

    @Test
    void testDebit_Success() {
        when(walletRepo.findByUserEmail(userEmail)).thenReturn(Optional.of(testWallet));
        
        walletService.debit(userEmail, 100.0, "Purchase", "ORD456");
        
        assertEquals(400.0, testWallet.getBalance());
        verify(transactionRepo).save(any(WalletTransaction.class));
    }

    @Test
    void testDebit_InsufficientFunds() {
        when(walletRepo.findByUserEmail(userEmail)).thenReturn(Optional.of(testWallet));
        
        assertThrows(APIException.class, () -> 
            walletService.debit(userEmail, 1000.0, "Purchase", "ORD789")
        );
    }

    @Test
    void testGetBalance() {
        when(walletRepo.findByUserEmail(userEmail)).thenReturn(Optional.of(testWallet));
        
        double balance = walletService.getBalance(userEmail);
        
        assertEquals(500.0, balance);
    }
}
