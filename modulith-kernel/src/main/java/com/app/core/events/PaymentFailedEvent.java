package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Published when a payment fails.
 * 
 * Publishers: modulith-checkout (PaymentService)
 * Listeners: modulith-marketing (sends retry email), modulith-analytics (tracks
 * failures)
 * 
 * @param paymentId     Payment attempt identifier
 * @param orderId       Associated order ID
 * @param userId        User who attempted payment
 * @param amount        Payment amount
 * @param paymentMethod Payment method attempted
 * @param reason        Failure reason
 * @param failedAt      When payment failed
 */
public record PaymentFailedEvent(
        String paymentId,
        Long orderId,
        Long userId,
        double amount,
        String paymentMethod,
        String reason,
        LocalDateTime failedAt) {
    public PaymentFailedEvent(String paymentId, Long orderId, Long userId, double amount,
            String paymentMethod, String reason) {
        this(paymentId, orderId, userId, amount, paymentMethod, reason, LocalDateTime.now());
    }
}
