package com.app.finance.payment;

import com.app.finance.entities.Payment;
import com.app.finance.repositories.PaymentRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Autonomous Payment Reconciliation Service.
 * Periodically checks for 'Stuck' payments (created status) and verifies their
 * real status with the Payment Gateway.
 */
@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private final PaymentRepo paymentRepo;
    private final PaymentService paymentService;

    public PaymentReconciliationService(PaymentRepo paymentRepo, PaymentService paymentService) {
        this.paymentRepo = paymentRepo;
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void reconcilePayments() {
        log.info("Starting Autonomous Payment Reconciliation...");

        // Find payments that are still in 'created' status
        List<Payment> stuckPayments = paymentRepo.findAll().stream()
                .filter(p -> p.getPgStatus() != null && "created".equalsIgnoreCase(p.getPgStatus()))
                .toList();

        for (Payment payment : stuckPayments) {
            reconcilePayment(payment);
        }
    }

    private void reconcilePayment(Payment payment) {
        if (payment.getPgOrderId() == null) {
            return;
        }

        log.info("Checking status for Payment {} (PG Order: {})", payment.getPaymentId(), payment.getPgOrderId());

        try {
            // Check status with Gateway
            Map<String, Object> details = paymentService.getPaymentDetails(payment.getPgOrderId());
            String status = (String) details.get("status");

            if ("captured".equalsIgnoreCase(status) || "paid".equalsIgnoreCase(status)) {
                log.warn("Self-Healing Triggered: Payment {} is PAID in Gateway but 'created' in DB. Syncing now.",
                        payment.getPaymentId());

                // Trigger capture logic which will emit the OrderPaidEvent
                paymentService.processPaymentCapture(payment.getPgOrderId(), (String) details.get("id"));

                log.info("Payment {} successfully reconciled.", payment.getPaymentId());
            }
        } catch (Exception e) {
            log.error("Failed to reconcile Payment {}: {}", payment.getPaymentId(), e.getMessage());
        }
    }
}
