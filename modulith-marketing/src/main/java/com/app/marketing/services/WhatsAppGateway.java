package com.app.marketing.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Gateway for WhatsApp Business API.
 * Currently a stub for development.
 */
@Service
@Slf4j
public class WhatsAppGateway {

    public void sendMessage(String phoneNumber, String message) {
        // Integration with Meta/Twilio would go here.
        log.info("Creating WhatsApp Message to {}: {}", phoneNumber, message);
    }
}
