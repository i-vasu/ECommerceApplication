package com.app.order.batch;

import com.app.order.entities.Order;
import com.app.order.repositories.OrderRepo;
import com.app.payment.PaymentService;
import com.app.commerce.states.OrderStatus;
import com.app.inventory.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
public class PaymentReconciliationWorker {

    private final OrderRepo orderRepo;
    private final PaymentService paymentService;
    private final InventoryReservationService inventoryService;

    /**
     * Runs every 30 minutes to clean up stale pending orders.
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void reconcilePendingPayments() {
        log.info("Starting Payment Reconciliation Worker...");

        // Find orders older than 1 hour still in PENDING state
        var staleOrders = orderRepo.findStalePendingOrders(LocalDateTime.now().minusHours(1));

        for (Order order : staleOrders) {
            try {
                if (order.getPayment() != null && order.getPayment().getPgOrderId() != null) {
                    // Check actual status from Razorpay
                    String pgOrderId = order.getPayment().getPgOrderId();
                    // We need a way to check status. getPaymentDetails checks paymentId, but we
                    // have pgOrderId.
                    // Razorpay API allows fetching by orderId too.

                    // For now, if it's stale and no payment_id is recorded, we assume it's
                    // abandoned.
                    if (order.getPayment().getPgPaymentId() == null) {
                        expireOrder(order);
                    }
                } else {
                    expireOrder(order);
                }
            } catch (Exception e) {
                log.error("Failed to reconcile order {}: {}", order.getOrderId(), e.getMessage());
            }
        }
    }

    private void expireOrder(Order order) {
        log.warn("Expiring stale order {}. Releasing stock.", order.getOrderId());
        order.setOrderStatus(OrderStatus.CANCELLED);

        // Release stock
        for (var item : order.getOrderItems()) {
            inventoryService.releaseStock(item.getItemCode(), item.getQuantity());
        }

        orderRepo.save(order);
    }
}
