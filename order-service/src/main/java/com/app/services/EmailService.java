package com.app.services;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);

    void sendOrderConfirmation(String to, Long orderId, Double amount, String paymentId);
}
