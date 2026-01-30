package com.app.marketing.services;

import com.app.core.events.OrderPaidEvent;
import com.app.core.events.UserRegisteredEvent;
import com.app.marketing.entities.CampaignLink;
import com.app.marketing.repositories.CampaignLinkRepo;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.HashMap;

@Service
@lombok.extern.slf4j.Slf4j
public class MarketingServiceImpl implements MarketingService {

    @org.springframework.beans.factory.annotation.Autowired
    private org.thymeleaf.TemplateEngine templateEngine;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.mail.javamail.JavaMailSender emailSender;

    @org.springframework.beans.factory.annotation.Autowired
    private CampaignLinkRepo campaignLinkRepo;

    @org.springframework.beans.factory.annotation.Autowired
    private JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("whatsAppGatewayImpl")
    private NotificationGateway whatsAppGateway;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("pushNotificationGatewayImpl")
    private NotificationGateway pushGateway;

    @Override
    public void sendOrderSuccessEvent(OrderPaidEvent event) {
        log.info("Internal Marketing: Tracking Order Success for {}", event.email());
    }

    @Override
    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Internal Marketing: Tracking User Registration for {}", event.email());
    }

    @Override
    public void sendCustomEvent(String email, String eventName, Map<String, Object> properties) {
        log.info("Internal Marketing: Custom Event '{}' for user {}", eventName, email);
    }

    @Override
    public void sendCampaignEmail(String campaignName, String recipientEmail, String subject, String templateName,
            Map<String, Object> variables) {
        
        // Check for unsubscription
        Boolean isUnsubscribed = jdbcTemplate.queryForObject(
            "SELECT unsubscribed FROM users WHERE email = ?", Boolean.class, recipientEmail);
        
        if (Boolean.TRUE.equals(isUnsubscribed)) {
            log.warn("Skipping marketing email for {} (Unsubscribed)", recipientEmail);
            return;
        }

        try {
            jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true, "UTF-8");

            Map<String, Object> enrichedVariables = new HashMap<>(variables);
            
            // Generate tracked unsubscription link
            enrichedVariables.put("unsubscribeUrl", "http://localhost:8080/api/mkt/unsubscribe/" + recipientEmail);
            
            // Wrap any 'ctaUrl' in variables with a tracking link
            if (enrichedVariables.containsKey("ctaUrl")) {
                String originalUrl = (String) enrichedVariables.get("ctaUrl");
                CampaignLink link = campaignLinkRepo.save(new CampaignLink(originalUrl, campaignName, recipientEmail));
                enrichedVariables.put("trackedCtaUrl", "http://localhost:8080/api/mkt/c/" + link.getLinkId());
            }

            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariables(enrichedVariables);

            String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom("marketing@fashionstore.com");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("Campaign Email '{}' sent to {}", campaignName, recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send campaign email: {}", e.getMessage());
        }
    }

    @Override
    public void sendCampaignWhatsApp(String campaignName, String mobileNumber, String messageTemplate,
            Map<String, String> variables) {
        whatsAppGateway.sendTemplate(mobileNumber, messageTemplate, variables);
        log.info("WhatsApp Campaign '{}' triggered for {}", campaignName, mobileNumber);
    }

    @Override
    public void sendCampaignPush(String campaignName, String recipientToken, String message) {
        pushGateway.sendMessage(recipientToken, message);
        log.info("Push Campaign '{}' triggered for {}", campaignName, recipientToken);
    }
}
