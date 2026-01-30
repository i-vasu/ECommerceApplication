package com.app.core.events;

/**
 * Event published when a user requests a password reset.
 */
public record ForgotPasswordEvent(
    String email,
    String token
) {}
