package com.app.marketing.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService, com.app.core.contracts.EmailServiceContract {

    @Autowired
    private org.thymeleaf.TemplateEngine templateEngine;

    @Autowired
    private JavaMailSender emailSender;

    @Override
    @Async
    public void sendSimpleMessage(String to, String subject, String text) {
        // Fallback or legacy support
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@fashionstore.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    @Async
    public void sendOrderConfirmation(String to, Long orderId, java.math.BigDecimal amount, String paymentId) {
        try {
            jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true, "UTF-8");

            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("orderId", orderId);
            context.setVariable("amount", amount);
            context.setVariable("paymentId", paymentId);

            String htmlContent = templateEngine.process("order-confirmation", context);

            helper.setFrom("noreply@fashionstore.com");
            helper.setTo(to);
            helper.setSubject("Order Confirmed - #" + orderId);
            helper.setText(htmlContent, true);

            emailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace(); // Log error properly in real app
        }
    }

    // EmailServiceContract implementations
    @Override
    @Async
    public void sendOrderConfirmation(String toEmail, String subject, String body) {
        sendSimpleMessage(toEmail, subject, body);
    }

    @Override
    @Async
    public void sendPaymentNotification(String toEmail, Long orderId, Double amount, String paymentId) {
        sendOrderConfirmation(toEmail, orderId, java.math.BigDecimal.valueOf(amount), paymentId);
    }

    @Override
    @Async
    public void sendEmail(String toEmail, String subject, String body) {
        sendSimpleMessage(toEmail, subject, body);
    }
}
