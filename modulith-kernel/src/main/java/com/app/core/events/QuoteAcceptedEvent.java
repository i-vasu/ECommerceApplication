package com.app.core.events;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record QuoteAcceptedEvent(
    Long quoteId,
    Long orderId,
    String customerEmail,
    LocalDateTime acceptedAt
) {}
