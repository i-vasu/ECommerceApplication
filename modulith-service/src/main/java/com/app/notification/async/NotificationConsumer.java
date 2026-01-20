package com.app.notification.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.app.order.repositories.OrderRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.order.entites.Order;

@Component
public class NotificationConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            String streamKey = message.getStream();
            String payload = message.getValue();

            if (streamKey != null && streamKey.contains("orders_stream")) {
               // Order Created Event (Payload is Order ID String)
               Long orderId = Long.valueOf(payload);
               Order order = orderRepo.findById(orderId).orElse(null);
               if (order != null) {
                   System.out.println(">>> [Notification] Sending Order Confirmation Email to: " + order.getEmail());
                   // Mock Email Service Call
               }
            } else if (streamKey != null && streamKey.contains("payment_events")) {
                System.out.println(">>> [Notification] Sending Payment Receipt Email. Payload: " + payload);
            } else if (streamKey != null && streamKey.contains("order_status_events")) {
                // Deserialize
                com.app.core.events.OrderStatusEvent event = objectMapper.readValue(payload, com.app.core.events.OrderStatusEvent.class);
                System.out.println(">>> [Notification] Sending Status Update Email (" + event.getStatus() + ") to: " + event.getUserEmail());
                if (event.getTrackingNumber() != null) {
                     System.out.println("      Tracking: " + event.getCarrier() + " " + event.getTrackingNumber());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error processing notification event: " + e.getMessage());
        }
    }
}
