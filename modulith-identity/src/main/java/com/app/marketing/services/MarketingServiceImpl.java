package com.app.marketing.services;

import com.app.core.events.OrderPaidEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
@lombok.extern.slf4j.Slf4j
public class MarketingServiceImpl implements MarketingService {

    private final RestClient restClient;

    @Value("${dittofeed.api.url:https://dittofeed.com/api/public/track}")
    private String dittofeedUrl;

    @Value("${dittofeed.api.key:}")
    private String apiKey;

    public MarketingServiceImpl() {
        this.restClient = RestClient.builder().build();
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.thymeleaf.TemplateEngine templateEngine;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.mail.javamail.JavaMailSender emailSender;

    @Override
    public void sendOrderSuccessEvent(OrderPaidEvent event) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Dittofeed API Key missing. Logging event: Order #{} for {}", event.orderId(), event.email());
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", event.email());
            body.put("event", "Order Success");

            Map<String, Object> properties = new HashMap<>();
            properties.put("orderId", event.orderId());
            properties.put("amount", event.amount());
            properties.put("paymentId", event.pgPaymentId());

            body.put("properties", properties);

            restClient.post()
                    .uri(dittofeedUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Marketing event sent to Dittofeed for Order: {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to send event to Dittofeed: {}", e.getMessage());
        }
    }

    @Override
    public void sendCampaignEmail(String campaignName, String recipientEmail, String subject, String templateName,
            Map<String, Object> variables) {
        try {
            jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true, "UTF-8");

            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom("marketing@fashionstore.com");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml

            emailSender.send(message);
            log.info("Campaign Email '{}' sent to {}", campaignName, recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send campaign email: {}", e.getMessage());
        }
    }

    @Override
    public void sendCampaignWhatsApp(String campaignName, String mobileNumber, String messageTemplate,
            Map<String, String> variables) {
        // In a real implementation, call Twilio/Guphsup/Interakt API
        // For now, we simulate by logging or sending a specific event to
        // Dittofeed/Segment
        log.info("[MOCK] Sending WhatsApp Campaign '{}' to {}", campaignName, mobileNumber);
        log.debug("    Template: {}", messageTemplate);
        log.debug("    Variables: {}", variables);

        // Example: POST to external provider
        /*
         * restClient.post().uri("https://api.whatsapp.provider.com/send")
         * .body(Map.of("to", mobileNumber, "template", messageTemplate, "vars",
         * variables))
         * .retrieve().toBodilessEntity();
         */
    }
}
