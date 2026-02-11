package com.app.order.batch;

import com.app.finance.payment.PaymentService;
import com.app.governance.states.OrderStatus;
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
    private final com.app.governance.states.OperationalStateMachineService operationalStateMachine;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

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
        log.warn("Expiring stale order {}. Triggering state machine.", order.getOrderId());
        
        // GAP-03: Use State Machine for consistent lifecycle management
        try {
            operationalStateMachine.triggerOrderEvent(order.getOrderId(), com.app.governance.states.OrderEvent.CANCEL);
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepo.save(order);
            
            // Note: Inventory release and other cleanups are handled by event listeners responding to OrderCancelledEvent
            // which should be published by OrderServiceImpl or here if we publish manually.
            // Let's publish manually to be safe if the state machine trigger doesn't do it.
            var items = order.getOrderItems().stream()
                    .map(item -> new com.app.core.events.OrderCancelledEvent.CancelledItem(item.getItemCode(), item.getQuantity()))
                    .toList();
            
            eventPublisher.publishEvent(new com.app.core.events.OrderCancelledEvent(
                    order.getOrderId(),
                    order.getUserId(),
                    order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0,
                    "Expired due to non-payment (Stale Order)",
                    items
            ));

        } catch (Exception e) {
            log.error("Failed to expire order {}: {}", order.getOrderId(), e.getMessage());
        }
    }
}
