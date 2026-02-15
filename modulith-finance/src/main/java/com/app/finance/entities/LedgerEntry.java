package com.app.finance.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
@Audited
@Data
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType type; // DEBIT or CREDIT

    @Column(name = "account_type")
    private String accountType; // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE

    private String description;
    private String referenceId;

    @CreationTimestamp
    private LocalDateTime entryDate;

    public enum EntryType {
        DEBIT, CREDIT
    }
}
