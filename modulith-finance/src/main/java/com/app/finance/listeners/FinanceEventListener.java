package com.app.finance.listeners;

import com.app.core.events.OrderCancelledEvent;
import com.app.finance.payment.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class FinanceEventListener {

    private static final Logger log = LoggerFactory.getLogger(FinanceEventListener.class);

    private final PaymentService paymentService;

    private final com.app.core.events.EventIdempotencyService idempotencyService;

    public FinanceEventListener(PaymentService paymentService, com.app.core.events.EventIdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @ApplicationModuleListener
    public void onOrderCancelled(OrderCancelledEvent event) {
        String eventId = "ORDER-CANCEL-FINANCE-" + event.orderId();
        if (idempotencyService.isEventProcessed(eventId, "FINANCE_MODULE", "OrderCancelledEvent")) {
            return;
        }

        log.info("Finance: Received OrderCancelledEvent for Order ID: {}. Initiating refund audit.", event.orderId());
        try {
            paymentService.processRefundForOrder(event.orderId(), event.reason());
            log.info("Finance: Refund process initiated for Order ID: {}", event.orderId());
            idempotencyService.markEventAsProcessed(eventId, "FINANCE_MODULE", "OrderCancelledEvent");

        } catch (Exception e) {
            log.error("Finance: Failed to initiate refund for Order ID: {}. Error: {}", event.orderId(),
                    e.getMessage());
        }
    }
}
