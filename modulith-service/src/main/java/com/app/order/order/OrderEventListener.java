package com.app.order.order;

import com.app.order.entities.Order;
import com.app.order.payloads.OrderPaidEvent;
import com.app.order.repositories.OrderRepo;
import com.app.shipping.ShipmentService;
import com.app.order.services.ERPNextService;
import com.app.marketing.services.EmailService;
import com.app.marketing.services.MarketingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Service
public class OrderEventListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final ERPNextService erpNextService;
    private final ShipmentService shipmentService;
    private final OrderRepo orderRepo;
    private final EmailService emailService;
    private final MarketingService marketingService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            var payload = message.getValue().get("payload");
            if (payload == null)
                return;

            var event = objectMapper.readValue(payload, OrderPaidEvent.class);
            log.info("Processing Order Paid Event (Stream) for Order: {}", event.orderId());

            var order = orderRepo.findById(event.orderId()).orElse(null);
            if (order == null) {
                log.error("Order not found for ID: {}", event.orderId());
                return;
            }

            // 1. Sync to ERPNext (Sales Order)
            try {
                erpNextService.createSalesOrderAsync(order);
            } catch (Exception e) {
                log.error("Failed to sync to ERPNext for Order {}: {}", event.orderId(), e.getMessage());
            }

            // 2. Create Shipment (Shiprocket/Shadowfax)
            try {
                shipmentService.createShipment(event.orderId());
            } catch (Exception e) {
                log.error("Failed to create shipment for Order {}: {}", event.orderId(), e.getMessage());
            }

            // 3. Send Email Notification
            try {
                emailService.sendOrderConfirmation(
                        event.email(),
                        event.orderId(),
                        event.amount(),
                        event.pgPaymentId());
            } catch (Exception e) {
                log.error("Failed to send email for Order {}: {}", event.orderId(), e.getMessage());
            }

            // 4. Send to Marketing (Dittofeed)
            try {
                marketingService.sendOrderSuccessEvent(event);
            } catch (Exception e) {
                log.error("Failed to send marketing event for Order {}: {}", event.orderId(), e.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to process order event stream message: {}", e.getMessage());
        }
    }
}
