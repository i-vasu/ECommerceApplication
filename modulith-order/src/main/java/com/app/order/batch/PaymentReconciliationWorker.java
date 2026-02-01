package com.app.order.batch;

import com.app.finance.payment.PaymentService;
import com.app.governance.states.OrderStatus;
import com.app.logistics.inventory.InventoryReservationService;
import com.app.order.entities.Order;
import com.app.order.repositories.OrderRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
                // Double check with Payment Service before expiring
                if (paymentService.isPaymentCompleted(order.getOrderId())) {
                    log.info("Order {} was actually paid but status not updated. Updating now.", order.getOrderId());
                    order.setOrderStatus(OrderStatus.PAYMENT_CAPTURED);
                    orderRepo.save(order);
                    continue;
                }

                // If it's stale and still PENDING (and not paid), we assume it's abandoned.
                expireOrder(order);
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
