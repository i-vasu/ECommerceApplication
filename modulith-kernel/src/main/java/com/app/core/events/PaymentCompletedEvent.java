package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Published when a payment is completed successfully.
 * 
 * Publishers: modulith-checkout (PaymentService)
 * Listeners: modulith-order (creates order), modulith-finance (records
 * transaction),
 * modulith-marketing (sends confirmation)
 * 
 * @param paymentId     Payment identifier
 * @param orderId       Associated order ID
 * @param userId        User who made payment
 * @param amount        Payment amount
 * @param paymentMethod Payment method used (COD, RAZORPAY, etc.)
 * @param transactionId External transaction ID
 * @param completedAt   When payment completed
 */
public record PaymentCompletedEvent(
        String paymentId,
        Long orderId,
        Long userId,
        double amount,
        String paymentMethod,
        String transactionId,
        LocalDateTime completedAt) {
    public PaymentCompletedEvent(String paymentId, Long orderId, Long userId, double amount,
            String paymentMethod, String transactionId) {
        this(paymentId, orderId, userId, amount, paymentMethod, transactionId, LocalDateTime.now());
    }
}
