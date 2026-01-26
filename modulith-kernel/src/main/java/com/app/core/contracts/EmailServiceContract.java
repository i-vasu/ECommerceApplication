package com.app.core.contracts;

/**
 * Email Service Contract - Exposes email operations to other modules
 * This interface allows other modules to send emails
 * without direct dependencies on the Marketing module.
 */
public interface EmailServiceContract {

    /**
     * Send order confirmation email
     */
    void sendOrderConfirmation(String toEmail, String subject, String body);

    /**
     * Send payment success notification
     */
    void sendPaymentNotification(String toEmail, Long orderId, Double amount, String paymentId);

    /**
     * Send generic email
     */
    void sendEmail(String toEmail, String subject, String body);
}
