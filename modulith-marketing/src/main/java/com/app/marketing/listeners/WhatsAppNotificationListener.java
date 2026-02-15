package com.app.marketing.listeners;

import com.app.core.events.OrderCreatedEvent;
import com.app.core.events.ShipmentStatusUpdatedEvent;
import com.app.marketing.services.WhatsAppGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationListener {

    private final WhatsAppGateway whatsAppGateway;

    @Async
    @EventListener
    public void onOrderCreated(com.app.core.events.OrderCreatedEvent event) {
        log.info("Sending WhatsApp Order Confirmation for Order: {}", event.orderId());
        
        // Template: order_confirmation (customer_name, order_id, amount)
        // Note: You need to create this template in Meta Business Manager
        
        try {
            // Send template message
            whatsAppGateway.sendTemplateMessage(
                event.phone(), // Using the phone number we added to OrderCreatedEvent
                "order_confirmation",
                "en_US",
                List.of(
                    Map.of(
                        "type", "body",
                        "parameters", List.of(
                            Map.of("type", "text", "text", "Customer"), // Placeholder for name
                            Map.of("type", "text", "text", event.orderId().toString())
                        )
                    )
                )
            );
            
            // Fallback to text for dev/testing if templates aren't set up
            whatsAppGateway.sendTextMessage(
                event.phone(), 
                "Order #" + event.orderId() + " placed successfully! Amount: " + event.totalAmount()
            );

        } catch (Exception e) {
            log.error("Failed to send WhatsApp notification", e);
        }
    }
}
