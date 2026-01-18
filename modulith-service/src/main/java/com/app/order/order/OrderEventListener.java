package com.app.order.order;

import com.app.order.entites.Order;
import com.app.order.payloads.OrderPaidEvent;
import com.app.order.repositories.OrderRepo;
import com.app.shipping.ShipmentService;
import com.app.order.services.ERPNextService;
import com.app.marketing.services.EmailService;
import com.app.marketing.services.MarketingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderEventListener implements
        org.springframework.data.redis.stream.StreamListener<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderEventListener.class);

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
            log.info(">>> Processing Order Paid Event (Stream) for Order: {}", event.orderId());

            Order order = orderRepo.findById(event.orderId()).orElse(null);
            if (order == null) {
                log.error("Order not found for ID: {}", event.orderId());
                return;
            }

            // 1. Sync to ERPNext (Sales Order)
            try {
                erpNextService.createSalesOrderAsync(order);
            } catch (Exception e) {
                log.error("Failed to sync to ERPNext for Order {}", event.orderId(), e);
            }

            // 2. Create Shipment (Shiprocket/Shadowfax)
            try {
                shipmentService.createShipment(event.orderId());
            } catch (Exception e) {
                log.error("Failed to create shipment for Order {}", event.orderId(), e);
            }

            // 3. Send Email Notification
            try {
                emailService.sendOrderConfirmation(
                        event.email(),
                        event.orderId(),
                        event.amount(),
                        event.pgPaymentId());
            } catch (Exception e) {
                log.error("Failed to send email for Order {}", event.orderId(), e);
            }

            // 4. Send to Marketing (Dittofeed)
            try {
                marketingService.sendOrderSuccessEvent(event);
            } catch (Exception e) {
                log.error("Failed to send marketing event for Order {}", event.orderId(), e);
            }

        } catch (Exception e) {
            log.error("Failed to process order event stream message", e);
        }
    }
}
