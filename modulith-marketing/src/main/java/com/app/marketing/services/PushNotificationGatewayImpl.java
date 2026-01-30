package com.app.marketing.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Log4j2
@Service
public class PushNotificationGatewayImpl implements NotificationGateway {

    @Value("${marketing.fcm.server-key:MOCK_FCM_KEY}")
    private String serverKey;

    @Override
    public void sendMessage(String recipientToken, String message) {
        log.info("Push Gateway (FCM): Sending notification to token {}: {}", recipientToken, message);
        // Real implementation would use Google's Firebase Admin SDK or RestTemplate to hit fcm.googleapis.com
        log.debug("Targeting FCM API with Server Key: {}", serverKey);
    }

    @Override
    public void sendTemplate(String recipientToken, String templateId, Map<String, String> variables) {
        log.info("Push Gateway (FCM): Sending template notification '{}' to {} with vars: {}", 
            templateId, recipientToken, variables);
    }
}
