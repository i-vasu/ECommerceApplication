package com.app.services;

import com.app.entites.Order;
import com.app.payloads.OrderPaidEvent;
import com.app.repositories.OrderRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@lombok.extern.slf4j.Slf4j
public class OrderEventListener implements
        org.springframework.data.redis.stream.StreamListener<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> {

    @Autowired
    private ERPNextService erpNextService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private EmailService emailService;

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
            log.info(">>> Processing Order Paid Event (Stream) for Order: {}", event.getOrderId());

            Order order = orderRepo.findById(event.getOrderId()).orElse(null);
            if (order == null) {
                log.error("Order not found for ID: {}", event.getOrderId());
                return;
            }

            // 1. Sync to ERPNext (Sales Order)
            try {
                erpNextService.createSalesOrder(order);
            } catch (Exception e) {
                log.error("Failed to sync to ERPNext for Order {}", event.getOrderId(), e);
            }

            // 2. Create Shipment (Shiprocket/Shadowfax)
            try {
                shipmentService.createShipment(event.getOrderId());
            } catch (Exception e) {
                log.error("Failed to create shipment for Order {}", event.getOrderId(), e);
            }

            // 3. Send Email Notification
            try {
                emailService.sendOrderConfirmation(
                        event.getEmail(),
                        event.getOrderId(),
                        event.getAmount(),
                        event.getPgPaymentId());
            } catch (Exception e) {
                log.error("Failed to send email for Order {}", event.getOrderId(), e);
            }

            // 4. Send to Marketing (Dittofeed)
            try {
                marketingService.sendOrderSuccessEvent(event);
            } catch (Exception e) {
                log.error("Failed to send marketing event for Order {}", event.getOrderId(), e);
            }

        } catch (Exception e) {
            log.error("Failed to process order event stream message", e);
        }
    }
}
