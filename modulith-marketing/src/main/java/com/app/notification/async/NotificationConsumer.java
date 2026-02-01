package com.app.marketing.notification.async;

import com.app.core.events.OrderConfirmedEvent;
import com.app.core.events.OrderStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@RequiredArgsConstructor
@Component
public class NotificationConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            var streamKey = message.getStream();
            var payload = message.getValue();

            if (streamKey != null && streamKey.contains("order_confirmed")) {
                var event = objectMapper.readValue(payload, OrderConfirmedEvent.class);
                log.info("Sending Order Confirmation Email to: {} for Order #{}", 
                    event.getCustomerEmail(), event.getOrderId());
                // Mock Email Service Call
                sendOrderConfirmationEmail(event);
            } else if (streamKey != null && streamKey.contains("payment_events")) {
                log.info("Sending Payment Receipt Email. Payload: {}", payload);
            } else if (streamKey != null && streamKey.contains("order_status_events")) {
                var event = objectMapper.readValue(payload, OrderStatusEvent.class);
                log.info("Sending Status Update Email for Order #{}: {}", 
                    event.orderId(), event.status());
            }

        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage());
        }
    }
    
    private void sendOrderConfirmationEmail(OrderConfirmedEvent event) {
        // Email service integration would go here
        log.debug("Email sent to {} for order {} (Amount: ${})", 
            event.getCustomerEmail(), event.getOrderId(), event.getTotalAmount());
    }
}
