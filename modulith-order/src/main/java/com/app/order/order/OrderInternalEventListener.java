package com.app.order.order;

import com.app.core.events.OrderPaidEvent;
import com.app.core.events.OrderStatusEvent;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderInternalEventListener {

    private static final Logger log = LogManager.getLogger(OrderInternalEventListener.class);

    private final OrderService orderService;

    @EventListener
    public void handleOrderPaid(OrderPaidEvent event) {
        log.info("Internal Event: Order Paid received for Order ID: {}", event.orderId());
        try {
            orderService.updateOrderStatusInternal(event.orderId(), "PAYMENT_CAPTURED");
        } catch (Exception e) {
            log.error("Failed to update order status to PAYMENT_CAPTURED for Order {}: {}",
                    event.orderId(), e.getMessage());
        }
    }

    @EventListener
    public void handleOrderStatusUpdate(OrderStatusEvent event) {
        log.info("Internal Event: Order Status Update received for Order ID: {} to {}",
                event.orderId(), event.status());
        try {
            orderService.updateOrderStatusInternal(event.orderId(), event.status());
        } catch (Exception e) {
            log.error("Failed to update order status to {} for Order {}: {}",
                    event.status(), event.orderId(), e.getMessage());
        }
    }
}
