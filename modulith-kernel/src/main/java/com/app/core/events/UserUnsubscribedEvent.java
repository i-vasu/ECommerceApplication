package com.app.core.events;

/**
 * Published when a user requests to unsubscribe from marketing communications.
 * Handled by the security module to update the user record.
 */
public record UserUnsubscribedEvent(String email) {}
