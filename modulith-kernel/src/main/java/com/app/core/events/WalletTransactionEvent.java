package com.app.core.events;

import java.math.BigDecimal;

/**
 * Domain event published when a wallet transaction (credit or debit) occurs.
 */
public record WalletTransactionEvent(
    String email,
    BigDecimal amount,
    String type, // CREDIT, DEBIT
    String description,
    String referenceId
) {}
