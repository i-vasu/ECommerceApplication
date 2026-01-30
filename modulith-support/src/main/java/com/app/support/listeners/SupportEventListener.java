package com.app.support.listeners;

import com.app.core.events.*;
import com.app.support.domain.SupportService;
import com.app.support.payloads.TicketDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listeners for Support module.
 * Automatically creates support tickets for failures and issues.
 */
@Component
@RequiredArgsConstructor
public class SupportEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SupportEventListener.class);

    private final SupportService supportService;

    /**
     * Handle payment failure - create support ticket if multiple failures.
     */
    @Async
    @EventListener
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Support: Monitoring payment failure for order {}", event.orderId());

        try {
            // Logic to create ticket for payment failure
            TicketDTO ticket = new TicketDTO();
            ticket.setUserEmail("customer@example.com"); // Get email from user service if needed
            ticket.setRelatedOrderId(event.orderId());
            ticket.setSubject("Payment Failure: Order #" + event.orderId());
            ticket.setMessage("Payment failed with reason: " + event.reason());

            supportService.createTicket(ticket);
            log.info("Support: Ticket created for payment failure - Order: {}", event.orderId());

        } catch (Exception e) {
            log.error("Support: Failed to process payment failure for order {} - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Handle order cancellation - check if support needed.
     */
    @Async
    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Support: Processing order cancellation {}", event.orderId());

        try {
            // Check cancellation reason
            if (requiresSupport(event.reason())) {
                log.info("Support: Creating follow-up ticket for cancelled order {} - Reason: {}",
                        event.orderId(), event.reason());

                TicketDTO ticket = new TicketDTO();
                ticket.setUserEmail("customer@example.com"); // Need to get email from somewhere if not in event
                ticket.setRelatedOrderId(event.orderId());
                ticket.setSubject("Order Cancellation Follow-up: Order #" + event.orderId());
                ticket.setMessage("Order was cancelled. Reason: " + event.reason());

                supportService.createTicket(ticket);
            }

        } catch (Exception e) {
            log.error("Support: Failed to process order cancellation {} - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Handle shipment delays - create proactive support ticket.
     */
    @Async
    @EventListener
    public void handleShipmentStatusUpdated(ShipmentStatusUpdatedEvent event) {
        if ("DELAYED".equals(event.newStatus()) || "EXCEPTION".equals(event.newStatus())) {
            log.info("Support: Creating ticket for shipment issue - Order: {}, Status: {}",
                    event.orderId(), event.newStatus());

            try {
                TicketDTO ticket = new TicketDTO();
                ticket.setUserEmail("customer@example.com");
                ticket.setRelatedOrderId(event.orderId());
                ticket.setSubject("Shipment " + event.newStatus() + ": Order #" + event.orderId());
                ticket.setMessage(
                        "Shipment status updated to " + event.newStatus() + ". Shipment ID: " + event.shipmentId());

                supportService.createTicket(ticket);

            } catch (Exception e) {
                log.error("Support: Failed to create ticket for shipment issue {} - {}",
                        event.shipmentId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Handle inventory low - alert support team.
     */
    @Async
    @EventListener
    public void handleInventoryLow(InventoryLowEvent event) {
        log.warn("Support: Low inventory alert for item {} - Current: {}, Threshold: {}",
                event.itemCode(), event.currentQuantity(), event.threshold());

        try {
            if (event.currentQuantity() == 0) {
                log.error("Support: STOCKOUT - Item {} is out of stock at {}",
                        event.itemCode(), event.warehouse());

                TicketDTO ticket = new TicketDTO();
                ticket.setUserEmail("inventory-alert@vaabhi.com");
                ticket.setSubject("STOCKOUT ALERT: Item " + event.itemCode());
                ticket.setMessage("Item " + event.itemCode() + " is out of stock at warehouse " + event.warehouse());

                supportService.createTicket(ticket);
            }

        } catch (Exception e) {
            log.error("Support: Failed to process inventory alert for {} - {}",
                    event.itemCode(), e.getMessage(), e);
        }
    }

    /**
     * Handle product search with zero results - capture customer need.
     */
    @Async
    @EventListener
    public void handleProductSearch(ProductSearchEvent event) {
        if (event.resultCount() == 0) {
            log.info("Support: Zero results for search '{}' - tracking customer need", event.query());

            try {
                // We keep it as a log or we could create a low-priority ticket/insight
                log.info("Support Insight: Customers are looking for '{}' but finding nothing.", event.query());
            } catch (Exception e) {
                log.error("Support: Failed to track zero-result search - {}", e.getMessage(), e);
            }
        }
    }

    private boolean requiresSupport(String reason) {
        // Reasons that need support follow-up
        return reason != null && (reason.toLowerCase().contains("damage") ||
                reason.toLowerCase().contains("defect") ||
                reason.toLowerCase().contains("wrong"));
    }
}
