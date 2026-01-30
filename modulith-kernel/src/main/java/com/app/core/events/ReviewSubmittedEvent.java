package com.app.core.events;

/**
 * Domain event published when a new product review is submitted.
 */
public record ReviewSubmittedEvent(
    Long productId,
    String email,
    Integer rating,
    String comment
) {}
