package com.app.security.services;

import com.app.security.entities.Wallet;
import com.app.security.entities.WalletTransaction;
import com.app.security.repositories.WalletRepo;
import com.app.security.repositories.WalletTransactionRepo;
import com.app.security.repositories.UserRepo;
import com.app.core.ResourceNotFoundException;
import com.app.core.APIException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletService {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(WalletService.class);

    private final WalletRepo walletRepo;
    private final WalletTransactionRepo transactionRepo;
    private final UserRepo userRepo;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final com.app.governance.states.OperationalStateMachineService stateMachineService;

    public WalletService(WalletRepo walletRepo, WalletTransactionRepo transactionRepo, UserRepo userRepo, 
                         org.springframework.context.ApplicationEventPublisher eventPublisher, 
                         com.app.governance.states.OperationalStateMachineService stateMachineService) {
        this.walletRepo = walletRepo;
        this.transactionRepo = transactionRepo;
        this.userRepo = userRepo;
        this.eventPublisher = eventPublisher;
        this.stateMachineService = stateMachineService;
    }

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

        // State Machine Governance
        stateMachineService.triggerWalletEvent(wallet.getWalletId(), com.app.governance.states.WalletEvent.CAPTURE);

        // DECOUPLED: Domain Event
        eventPublisher.publishEvent(new com.app.core.events.WalletTransactionEvent(
                email,
                java.math.BigDecimal.valueOf(amount),
                "CREDIT",
                description,
                referenceId
        ));

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

        // State Machine Governance
        stateMachineService.triggerWalletEvent(wallet.getWalletId(), com.app.governance.states.WalletEvent.CAPTURE);

        // DECOUPLED: Domain Event
        eventPublisher.publishEvent(new com.app.core.events.WalletTransactionEvent(
                email,
                java.math.BigDecimal.valueOf(amount),
                "DEBIT",
                description,
                referenceId
        ));

        log.info("Debited {} from wallet of {}. Ref: {}", amount, email, referenceId);
    }

    public List<WalletTransaction> getTransactions(String email) {
        Wallet wallet = getOrCreateWallet(email);
        return transactionRepo.findByWalletWalletId(wallet.getWalletId());
    }
}
