package com.app.marketing.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Log4j2
@Service
public class WhatsAppGatewayImpl implements NotificationGateway {

    @Value("${marketing.whatsapp.api-key:MOCK_KEY}")
    private String apiKey;

    @Value("${marketing.whatsapp.url:http://localhost:8081/mock-whatsapp}")
    private String apiUrl;

    @Override
    public void sendMessage(String recipient, String message) {
        log.info("WhatsApp Gateway: Sending message to {}: {}", recipient, message);
        // Real implementation would use RestTemplate/RestClient to call Twilio/Meta API
        // For production parity, we log the targeted URL
        log.debug("Targeting WhatsApp API: {} with API Key: {}", apiUrl, apiKey);
    }

    @Override
    public void sendTemplate(String recipient, String templateId, Map<String, String> variables) {
        log.info("WhatsApp Gateway: Sending template '{}' to {} with vars: {}", templateId, recipient, variables);
    }
}
