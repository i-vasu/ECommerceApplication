package com.app.identity.services;

import com.app.identity.entities.Wallet;
import com.app.identity.entities.WalletTransaction;
import com.app.identity.repositories.WalletRepo;
import com.app.identity.repositories.WalletTransactionRepo;
import com.app.identity.repositories.UserRepo;
import com.app.core.ResourceNotFoundException;
import com.app.core.APIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepo walletRepo;
    private final WalletTransactionRepo transactionRepo;
    private final UserRepo userRepo;

    @Transactional
    public Wallet getOrCreateWallet(String email) {
        return walletRepo.findByUserEmail(email)
                .orElseGet(() -> {
                    var user = userRepo.findByEmail(email)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
                    Wallet newWallet = new Wallet();
                    newWallet.setUser(user);
                    newWallet.setBalance(0.0);
                    return walletRepo.save(newWallet);
                });
    }

    public Double getBalance(String email) {
        return getOrCreateWallet(email).getBalance();
    }

    @Transactional
    public void credit(String email, Double amount, String description, String referenceId) {
        Wallet wallet = getOrCreateWallet(email);
        wallet.setBalance(wallet.getBalance() + amount);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(WalletTransaction.TransactionType.CREDIT);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);

        transactionRepo.save(transaction);
        walletRepo.save(wallet);
        log.info("Credited {} to wallet of {}. Ref: {}", amount, email, referenceId);
    }

    @Transactional
    public void debit(String email, Double amount, String description, String referenceId) {
        Wallet wallet = getOrCreateWallet(email);
        if (wallet.getBalance() < amount) {
            throw new APIException("Insufficient wallet balance");
        }

        wallet.setBalance(wallet.getBalance() - amount);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(WalletTransaction.TransactionType.DEBIT);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);

        transactionRepo.save(transaction);
        walletRepo.save(wallet);
        log.info("Debited {} from wallet of {}. Ref: {}", amount, email, referenceId);
    }

    public List<WalletTransaction> getTransactions(String email) {
        Wallet wallet = getOrCreateWallet(email);
        return transactionRepo.findByWalletWalletId(wallet.getWalletId());
    }
}
