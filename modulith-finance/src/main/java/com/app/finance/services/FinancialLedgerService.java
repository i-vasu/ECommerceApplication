package com.app.finance.services;

import com.app.finance.entities.LedgerEntry;
import com.app.finance.repositories.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FinancialLedgerService {

    private final LedgerEntryRepository ledgerRepo;

    /**
     * Records a double-entry transaction.
     * In accounting, every transaction must have at least one debit and one credit of equal amount.
     */
    @Transactional
    public void recordTransaction(String debitAcc, String creditAcc, BigDecimal amount, String desc, String refId) {
        // Validation: Sum of debits must equal sum of credits (simplified here for 1-to-1)
        createEntry(debitAcc, amount, LedgerEntry.EntryType.DEBIT, desc, refId);
        createEntry(creditAcc, amount, LedgerEntry.EntryType.CREDIT, desc, refId);
    }

    private void createEntry(String account, BigDecimal amount, LedgerEntry.EntryType type, String desc, String refId) {
        LedgerEntry entry = new LedgerEntry();
        entry.setAccountNumber(account);
        entry.setAmount(amount);
        entry.setType(type);
        entry.setDescription(desc);
        entry.setReferenceId(refId);
        
        // Auto-assign account types based on prefix
        if (account.startsWith("AST-")) entry.setAccountType("ASSET");
        else if (account.startsWith("LIA-")) entry.setAccountType("LIABILITY");
        else if (account.startsWith("REV-")) entry.setAccountType("REVENUE");
        else if (account.startsWith("EXP-")) entry.setAccountType("EXPENSE");
        
        ledgerRepo.save(entry);
    }

    public List<LedgerEntry> getEntriesByAccount(String account) {
        return ledgerRepo.findByAccountNumber(account);
    }

    public List<LedgerEntry> getAllEntries() {
        return ledgerRepo.findAll();
    }

    public BigDecimal getBalance(String account) {
        List<LedgerEntry> entries = ledgerRepo.findByAccountNumber(account);
        BigDecimal debits = entries.stream()
                .filter(e -> e.getType() == LedgerEntry.EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = entries.stream()
                .filter(e -> e.getType() == LedgerEntry.EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return debits.subtract(credits);
    }
}
