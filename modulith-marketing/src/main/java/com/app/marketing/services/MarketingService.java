package com.app.marketing.services;

import com.app.core.events.OrderPaidEvent;
import com.app.core.events.UserRegisteredEvent;

import java.util.Map;

public interface MarketingService {
    void sendOrderSuccessEvent(OrderPaidEvent event);

    void sendUserRegisteredEvent(UserRegisteredEvent event);

    void sendCustomEvent(String email, String eventName, Map<String, Object> properties);

    void sendCampaignEmail(String campaignName, String recipientEmail, String subject, String templateName,
            Map<String, Object> variables);

    void sendCampaignWhatsApp(String campaignName, String mobileNumber, String messageTemplate,
            Map<String, String> variables);

    void sendCampaignPush(String campaignName, String recipientToken, String message);
}
