package com.app.order.listeners;

import com.app.core.events.*;
import com.app.order.order.OrderService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listeners for Order module.
 * Handles payment completion, shipment updates, and cancellations.
 */
@Component
public class OrderModuleEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderModuleEventListener.class);

    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderModuleEventListener(OrderService orderService, ApplicationEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handle payment completion - create order from cart.
     * This is synchronous to ensure order is created in same transaction.
     */
    @EventListener
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Order: Creating order for payment {}", event.paymentId());

        try {
            // Order should already exist at this point, just update status
            var order = orderService.getOrderById(event.orderId());
            if (order != null) {
                orderService.confirmPayment(event.orderId(), event.transactionId());

                // Publish OrderCreatedEvent for downstream processing
                eventPublisher.publishEvent(new OrderCreatedEvent(
                        event.orderId(),
                        event.userId(),
                        order.email(),
                        order.shippingReceiverPhone(), // Added phone
                        order.totalAmount(),
                        order.orderItems().stream()
                                .map(item -> new OrderCreatedEvent.OrderItemData(
                                        item.product() != null ? item.product().itemCode() : "UNKNOWN",
                                        item.product() != null ? item.product().productName() : "UNKNOWN",
                                        item.quantity(),
                                        item.orderedProductPrice()))
                                .toList(),
                        java.time.LocalDateTime.now()));

                log.info("Order: Successfully confirmed order {} after payment", event.orderId());
            }
        } catch (Exception e) {
            log.error("Order: Failed to process payment completion for order {} - {}",
                    event.orderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create order from payment", e);
        }
    }

    /**
     * Handle payment failure - update order status.
     */
    @Async
    @EventListener
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Order: Handling payment failure for order {}", event.orderId());

        try {
            orderService.markPaymentFailed(event.orderId(), event.reason());
            log.info("Order: Marked order {} as payment failed", event.orderId());
        } catch (Exception e) {
            log.error("Order: Failed to update order {} after payment failure - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Handle shipment status updates - update order status accordingly.
     */
    @Async
    @EventListener
    public void handleShipmentStatusUpdated(ShipmentStatusUpdatedEvent event) {
        log.info("Order: Updating order {} for shipment status: {} -> {}",
                event.orderId(), event.oldStatus(), event.newStatus());

        try {
            // Map shipment status to order status
            String orderStatus = mapShipmentToOrderStatus(event.newStatus());
            if (orderStatus != null) {
                orderService.updateOrderStatus(event.orderId(), orderStatus);

                // If delivered, publish event
                if ("DELIVERED".equals(event.newStatus())) {
                    eventPublisher.publishEvent(new OrderStatusEvent(
                            event.orderId(),
                            null, // email not available
                            "DELIVERED",
                            event.trackingNumber(),
                            event.carrier(),
                            null)); // tenantId not available
                }

                log.info("Order: Updated order {} status to {}", event.orderId(), orderStatus);
            }
        } catch (Exception e) {
            log.error("Order: Failed to update order {} after shipment update - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Handle stock issues - may need to cancel order.
     */
    @Async
    @EventListener
    public void handleInventoryLowEvent(InventoryLowEvent event) {
        log.warn("Order: Inventory low for item {} - checking pending orders", event.itemCode());

        try {
            // Check if any pending orders are affected
            var affectedOrders = orderService.findPendingOrdersByItemCode(event.itemCode());
            for (var order : affectedOrders) {
                log.warn("Order: Order {} may be affected by low inventory for {}",
                        order.orderId(), event.itemCode());
                // Could automatically cancel or notify customer
            }
        } catch (Exception e) {
            log.error("Order: Failed to handle inventory low event for {} - {}",
                    event.itemCode(), e.getMessage(), e);
        }
    }

    /**
     * Map shipment status to order status.
     */
    private String mapShipmentToOrderStatus(String shipmentStatus) {
        return switch (shipmentStatus) {
            case "SHIPPED" -> "SHIPPED";
            case "IN_TRANSIT" -> "SHIPPED";
            case "OUT_FOR_DELIVERY" -> "SHIPPED";
            case "DELIVERED" -> "DELIVERED";
            case "CANCELLED" -> "CANCELLED";
            case "RETURNED" -> "RETURNED";
            default -> null; // Don't update for other statuses
        };
    }
}
