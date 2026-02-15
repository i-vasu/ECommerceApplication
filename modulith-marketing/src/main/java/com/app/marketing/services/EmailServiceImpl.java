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

    @Override
    @Async
    public void sendShipmentTracking(String to, Long orderId, String trackingNumber, String carrier) {
        try {
            jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true, "UTF-8");

            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("orderId", orderId);
            context.setVariable("trackingNumber", trackingNumber);
            context.setVariable("carrier", carrier);
            context.setVariable("supportEmail", "support@fashionstore.com");

            // We should create a 'shipment-tracking.html' template, but for now fallback to simple message if template missing
            // String htmlContent = templateEngine.process("shipment-tracking", context);
            
            String htmlContent = "<h1>Order #" + orderId + " Shipped!</h1>" +
                    "<p>Your order has been shipped via " + carrier + ".</p>" +
                    "<p>Tracking Number: <strong>" + trackingNumber + "</strong></p>";

            helper.setFrom("noreply@fashionstore.com");
            helper.setTo(to);
            helper.setSubject("Your Order #" + orderId + " is on the way!");
            helper.setText(htmlContent, true);

            emailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Async
    public void sendReviewRequest(String to, Long orderId) {
         try {
            jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true, "UTF-8");

            String htmlContent = "<h1>How was your recent order #" + orderId + "?</h1>" +
                    "<p>We'd love to hear your feedback!</p>" +
                    "<a href='https://fashionstore.com/reviews/new?orderId=" + orderId + "'>Leave a Review</a>";

            helper.setFrom("noreply@fashionstore.com");
            helper.setTo(to);
            helper.setSubject("Rate your purchase - Order #" + orderId);
            helper.setText(htmlContent, true);

            emailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
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
