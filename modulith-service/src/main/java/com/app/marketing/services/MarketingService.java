package com.app.marketing.services;

import com.app.order.payloads.OrderPaidEvent;
import java.util.Map;

public interface MarketingService {
        void sendOrderSuccessEvent(OrderPaidEvent event);

        void sendCampaignEmail(String campaignName, String recipientEmail, String subject, String templateName,
                        Map<String, Object> variables);

        void sendCampaignWhatsApp(String campaignName, String mobileNumber, String messageTemplate,
                        Map<String, String> variables);
}
