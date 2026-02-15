package com.app.finance.repositories;

import com.app.finance.entities.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByAccountNumber(String accountNumber);
    List<LedgerEntry> findByReferenceId(String referenceId);
}
