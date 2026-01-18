package com.app.marketing.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private org.thymeleaf.TemplateEngine templateEngine;

    @Autowired
    private JavaMailSender emailSender;

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        // Fallback or legacy support
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@fashionstore.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    public void sendOrderConfirmation(String to, Long orderId, Double amount, String paymentId) {
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
}
