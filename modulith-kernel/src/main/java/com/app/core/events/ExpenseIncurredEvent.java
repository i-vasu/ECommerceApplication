package com.app.core.events;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ExpenseIncurredEvent(
    Long expenseId,
    String category,
    Double amount,
    String description,
    String paymentMethod,
    LocalDateTime incurredAt
) {}
