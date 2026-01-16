package com.app.services;

import com.app.payloads.OrderPaidEvent;

public interface MarketingService {
    void sendOrderSuccessEvent(OrderPaidEvent event);

    void sendCampaignEmail(String campaignName, String recipientEmail, String subject, String templateName,
            java.util.Map<String, Object> variables);

    void sendCampaignWhatsApp(String campaignName, String mobileNumber, String messageTemplate,
            java.util.Map<String, String> variables);
}
