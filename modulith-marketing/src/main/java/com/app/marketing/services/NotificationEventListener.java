package com.app.marketing.services;

import com.app.core.events.ForgotPasswordEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;
    private final MarketingService marketingService;
    private final WorkflowEngine workflowEngine;

    @ApplicationModuleListener
    public void onForgotPassword(ForgotPasswordEvent event) {
        log.info("NotificationEventListener: Sending password reset email to: {}", event.email());
        try {
            emailService.sendSimpleMessage(
                event.email(), 
                "Password Reset Request", 
                "Your password reset token is: " + event.token() + ". It will expire in 30 minutes."
            );
            log.info("Password reset email sent successfully to: {}", event.email());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}. Error: {}", event.email(), e.getMessage());
        }
    }

    @ApplicationModuleListener
    public void onUserRegistered(com.app.core.events.UserRegisteredEvent event) {
        log.info("NotificationEventListener: Processing Welcome sequence for: {}", event.email());
        try {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("firstName", event.firstName());
            
            marketingService.sendCampaignEmail(
                "WELCOME_SERIES",
                event.email(),
                "Welcome to Vaabhi!",
                "emails/welcome",
                vars
            );

            // Start Journey Workflow
            workflowEngine.triggerWorkflows("USER_REGISTERED", event.email(), new java.util.HashMap<>());
        } catch (Exception e) {
            log.error("Failed to execute welcome sequence for: {}. Error: {}", event.email(), e.getMessage());
        }
    }

    @ApplicationModuleListener
    public void onOrderPaid(com.app.core.events.OrderPaidEvent event) {
        log.info("NotificationEventListener: Processing post-purchase sequence for order: {}", event.orderId());
        try {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("orderId", event.orderId());
            vars.put("amount", event.amount());
            vars.put("paymentMethod", event.paymentMethod());
            
            marketingService.sendCampaignEmail(
                "POST_PURCHASE_CONFIRMATION",
                event.email(),
                "Order Confirmed - #" + event.orderId(),
                "emails/order-confirmation",
                vars
            );

            // Start Journey Workflow
            workflowEngine.triggerWorkflows("ORDER_PAID", event.email(), new java.util.HashMap<>());
        } catch (Exception e) {
            log.error("Failed to execute post-purchase sequence for order {}: {}", event.orderId(), e.getMessage());
        }
    }
}

