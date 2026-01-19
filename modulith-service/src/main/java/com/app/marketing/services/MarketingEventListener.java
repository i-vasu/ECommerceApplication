package com.app.marketing.services;

import com.app.order.payloads.OrderPaidEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketingEventListener implements
        org.springframework.data.redis.stream.StreamListener<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MarketingEventListener.class);

    @Autowired
    private MarketingService marketingService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public void onMessage(org.springframework.data.redis.connection.stream.MapRecord<String, String, String> message) {
        try {
            String payload = message.getValue().get("payload");
            if (payload == null)
                return;

            OrderPaidEvent event = objectMapper.readValue(payload, OrderPaidEvent.class);

            log.info(">>> Consumer: Processing Marketing Event (Stream) for Order {}", event.orderId());
            marketingService.sendOrderSuccessEvent(event);

        } catch (Exception e) {
            log.error("Failed to process marketing stream message", e);
        }
    }
}
