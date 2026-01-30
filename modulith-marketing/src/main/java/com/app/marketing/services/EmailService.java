package com.app.marketing.services;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);

    void sendOrderConfirmation(String to, Long orderId, java.math.BigDecimal amount, String paymentId);
}
