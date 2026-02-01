package com.app.finance.listeners;

import com.app.core.APIException;
import com.app.core.events.ReturnApprovedEvent;
import com.app.finance.entities.Payment;
import com.app.finance.payment.PaymentService;
import com.app.finance.repositories.PaymentRepo;
import com.app.security.services.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class PaymentReturnListener {

    private final PaymentService paymentService;
    private final WalletService walletService;
    private final PaymentRepo paymentRepo;

    /**
     * Listen for return approval and execute the refund.
     */
    @Async
    @EventListener
    public void handleReturnApproved(ReturnApprovedEvent event) {
        log.info("Finance: Processing refund for approved return on order {}", event.orderId());

        try {
            if ("WALLET".equalsIgnoreCase(event.refundType())) {
                walletService.credit(
                        event.email(),
                        event.refundAmount(),
                        "Refund for approved return (Order #" + event.orderId() + ")",
                        "RET_" + event.requestId());
                log.info("Finance: Wallet credited for return {}", event.requestId());
            } else {
                // Find PG Payment ID for the order
                Payment payment = paymentRepo.findByOrderId(event.orderId()).stream()
                        .findFirst()
                        .orElseThrow(() -> new APIException("Payment not found for order: " + event.orderId()));

                // Initiate Partial Refund via PG (Razorpay)
                paymentService.initiateRefund(
                        payment.getPgPaymentId(),
                        Math.round(event.refundAmount() * 100), // convert to paisa
                        "Partial return for order #" + event.orderId() + " (Request #" + event.requestId() + ")");
                log.info("Finance: PG refund initiated for return {}", event.requestId());
            }

        } catch (Exception e) {
            log.error("Finance: Failed to execute refund for return {} - {}", event.requestId(), e.getMessage());
        }
    }
}
