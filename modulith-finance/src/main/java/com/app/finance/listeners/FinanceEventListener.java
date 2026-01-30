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

    public FinanceEventListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @ApplicationModuleListener
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Finance: Received OrderCancelledEvent for Order ID: {}. Initiating refund audit.", event.orderId());
        try {
            paymentService.processRefundForOrder(event.orderId(), event.reason());
            log.info("Finance: Refund process initiated for Order ID: {}", event.orderId());

        } catch (Exception e) {
            log.error("Finance: Failed to initiate refund for Order ID: {}. Error: {}", event.orderId(),
                    e.getMessage());
        }
    }
}
