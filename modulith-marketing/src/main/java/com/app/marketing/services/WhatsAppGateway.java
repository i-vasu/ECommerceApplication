package com.app.marketing.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway for WhatsApp Business API via Meta Cloud API.
 * Sends transactional messages to customers.
 */
@Service
@Slf4j
public class WhatsAppGateway {

    @Value("${meta.cloud.api.base-url:https://graph.facebook.com/v19.0}")
    private String metaApiBaseUrl;

    @Value("${meta.cloud.api.phone-number-id:}")
    private String phoneNumberId;

    @Value("${meta.cloud.api.access-token:}")
    private String accessToken;

    private final RestClient restClient;

    public WhatsAppGateway(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Send a free-form text message (only works within 24h customer-initiated window).
     */
    public void sendTextMessage(String toPhoneNumber, String message) {
        if (!isConfigured()) {
            log.warn("Meta Cloud API not configured. WhatsApp message skipped for {}", toPhoneNumber);
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("recipient_type", "individual");
            body.put("to", formatPhoneNumber(toPhoneNumber));
            body.put("type", "text");
            body.put("text", Map.of("preview_url", false, "body", message));

            sendToMeta(body);

        } catch (Exception e) {
            log.error("Error sending WhatsApp text message to {}: {}", toPhoneNumber, e.getMessage(), e);
        }
    }

    /**
     * Legacy method for backward compatibility.
     */
    public void sendMessage(String toPhoneNumber, String message) {
        sendTextMessage(toPhoneNumber, message);
    }

    /**
     * Send a template message (required for business-initiated conversations).
     */
    public void sendTemplateMessage(String toPhoneNumber, String templateName, String languageCode, List<Map<String, Object>> components) {
        if (!isConfigured()) {
            log.warn("Meta Cloud API not configured. WhatsApp template skipped for {}", toPhoneNumber);
            return;
        }

        try {
            Map<String, Object> template = new HashMap<>();
            template.put("name", templateName);
            template.put("language", Map.of("code", languageCode));
            
            if (components != null && !components.isEmpty()) {
                template.put("components", components);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("to", formatPhoneNumber(toPhoneNumber));
            body.put("type", "template");
            body.put("template", template);

            sendToMeta(body);

        } catch (Exception e) {
            log.error("Error sending WhatsApp template message to {}: {}", toPhoneNumber, e.getMessage(), e);
        }
    }

    private void sendToMeta(Map<String, Object> requestBody) {
        String url = String.format("%s/%s/messages", metaApiBaseUrl, phoneNumberId);

        String response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        log.info("WhatsApp message sent via Meta Cloud API. Response: {}", response);
    }

    private boolean isConfigured() {
        return phoneNumberId != null && !phoneNumberId.isBlank() && 
               accessToken != null && !accessToken.isBlank();
    }

    private String formatPhoneNumber(String phone) {
        // Remove spaces, hyphens
        String cleaned = phone.replaceAll("[^0-9]", "");
        // Ensure strictly digits? Usually Meta expects country code without +
        return cleaned;
    }
}
