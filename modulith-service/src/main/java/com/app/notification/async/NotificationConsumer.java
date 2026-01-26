package com.app.notification.async;

import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.app.order.repositories.OrderRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.core.events.OrderStatusEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Component
public class NotificationConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    private final OrderRepo orderRepo;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            var streamKey = message.getStream();
            var payload = message.getValue();

            if (streamKey != null && streamKey.contains("orders_stream")) {
                var orderId = Long.valueOf(payload);
                var order = orderRepo.findById(orderId).orElse(null);
                if (order != null) {
                    log.info("Sending Order Confirmation Email to: {}", order.getEmail());
                    // Mock Email Service Call
                }
            } else if (streamKey != null && streamKey.contains("payment_events")) {
                log.info("Sending Payment Receipt Email. Payload: {}", payload);
            } else if (streamKey != null && streamKey.contains("order_status_events")) {
                var event = objectMapper.readValue(payload, OrderStatusEvent.class);
                log.info("Sending Status Update Email ({}) to: {}", event.getStatus(), event.getUserEmail());
                if (event.getTrackingNumber() != null) {
                    log.info("      Tracking: {} {}", event.getCarrier(), event.getTrackingNumber());
                }
            }

        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage());
        }
    }
}
