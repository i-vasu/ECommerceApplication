package com.app.governance.listeners;

import com.app.core.events.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listeners for Governance module.
 * Handles audit logging, compliance tracking, and rule enforcement.
 */
@Component
public class GovernanceEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GovernanceEventListener.class);

    // TODO: Inject audit service
    // private final AuditService auditService;

    /**
     * Audit all order creation events for compliance.
     */
    @Async
    @EventListener
    public void auditOrderCreated(OrderCreatedEvent event) {
        log.info("Governance: Auditing order creation - Order: {}, User: {}, Amount: ${}",
                event.orderId(), event.userId(), event.totalAmount());

        try {
            // TODO: Create audit record
            createAuditLog(
                    "ORDER_CREATED",
                    event.orderId(),
                    event.userId(),
                    String.format("Order created with %d items, total $%.2f",
                            event.items().size(), event.totalAmount()));

        } catch (Exception e) {
            log.error("Governance: Failed to audit order creation {} - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Audit payment transactions for financial compliance.
     */
    @Async
    @EventListener
    public void auditPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Governance: Auditing payment - Order: {}, Amount: ${}, Method: {}",
                event.orderId(), event.amount(), event.paymentMethod());

        try {
            // TODO: Create audit record
            createAuditLog(
                    "PAYMENT_COMPLETED",
                    event.orderId(),
                    event.userId(),
                    String.format("Payment of $%.2f via %s, Transaction: %s",
                            event.amount(), event.paymentMethod(), event.transactionId()));

            // Check for suspicious transactions
            if (event.amount() > 10000.0) {
                log.warn("Governance: HIGH VALUE TRANSACTION - Order: {}, Amount: ${}",
                        event.orderId(), event.amount());
                flagForReview(event);
            }

        } catch (Exception e) {
            log.error("Governance: Failed to audit payment {} - {}",
                    event.paymentId(), e.getMessage(), e);
        }
    }

    /**
     * Audit payment failures for fraud detection.
     */
    @Async
    @EventListener
    public void auditPaymentFailed(PaymentFailedEvent event) {
        log.warn("Governance: Auditing payment failure - Order: {}, Reason: {}",
                event.orderId(), event.reason());

        try {
            // TODO: Create audit record
            createAuditLog(
                    "PAYMENT_FAILED",
                    event.orderId(),
                    event.userId(),
                    String.format("Payment failed: %s ($%.2f via %s)",
                            event.reason(), event.amount(), event.paymentMethod()));

            // Track for fraud detection
            checkForFraud(event);

        } catch (Exception e) {
            log.error("Governance: Failed to audit payment failure {} - {}",
                    event.paymentId(), e.getMessage(), e);
        }
    }

    /**
     * Audit order cancellations for pattern analysis.
     */
    @Async
    @EventListener
    public void auditOrderCancelled(OrderCancelledEvent event) {
        log.info("Governance: Auditing order cancellation - Order: {}, Reason: {}",
                event.orderId(), event.reason());

        try {
            // TODO: Create audit record
            createAuditLog(
                    "ORDER_CANCELLED",
                    event.orderId(),
                    event.userId(),
                    String.format("Order cancelled: %s (%d items)",
                            event.reason(), event.items().size()));

            // Track cancellation reasons for business intelligence
            trackCancellationReason(event.reason());

        } catch (Exception e) {
            log.error("Governance: Failed to audit order cancellation {} - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Audit product changes for inventory compliance.
     */
    @Async
    @EventListener
    public void auditProductUpdated(ProductUpdatedEvent event) {
        log.debug("Governance: Auditing product update - Product: {}, Item: {}",
                event.productId(), event.itemCode());

        try {
            // TODO: Create audit record for significant changes
            if (!event.oldQuantity().equals(event.newQuantity())) {
                createAuditLog(
                        "PRODUCT_QUANTITY_CHANGED",
                        event.productId(),
                        null, // System action
                        String.format("Quantity changed from %d to %d for %s",
                                event.oldQuantity(), event.newQuantity(), event.itemCode()));
            }

        } catch (Exception e) {
            log.error("Governance: Failed to audit product update {} - {}",
                    event.productId(), e.getMessage(), e);
        }
    }

    /**
     * Audit stock movements for inventory compliance.
     */
    @Async
    @EventListener
    public void auditStockUpdated(StockUpdatedEvent event) {
        log.debug("Governance: Auditing stock update - Item: {}, Source: {}",
                event.itemCode(), event.source());

        try {
            // TODO: Create audit record
            createAuditLog(
                    "STOCK_UPDATED",
                    null,
                    null,
                    String.format("Stock for %s changed from %d to %d (source: %s)",
                            event.itemCode(), event.oldQuantity(),
                            event.newQuantity(), event.source()));

            // Alert on unexpected stock decreases
            if ("MANUAL".equals(event.source()) && event.newQuantity() < event.oldQuantity()) {
                log.warn("Governance: Manual stock decrease detected for {}", event.itemCode());
            }

        } catch (Exception e) {
            log.error("Governance: Failed to audit stock update for {} - {}",
                    event.itemCode(), e.getMessage(), e);
        }
    }

    /**
     * Audit restock operations for procurement compliance.
     */
    @Async
    @EventListener
    public void auditRestockCompleted(RestockCompletedEvent event) {
        log.info("Governance: Auditing restock completion - PO: {}, Item: {}",
                event.purchaseOrderId(), event.itemCode());

        try {
            // TODO: Create audit record
            createAuditLog(
                    "RESTOCK_COMPLETED",
                    null,
                    null,
                    String.format("Restock completed: %d units of %s (PO: %s)",
                            event.receivedQuantity(), event.itemCode(), event.purchaseOrderId()));

        } catch (Exception e) {
            log.error("Governance: Failed to audit restock {} - {}",
                    event.purchaseOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Audit shipment status for logistics compliance.
     */
    @Async
    @EventListener
    public void auditShipmentUpdate(ShipmentStatusUpdatedEvent event) {
        log.debug("Governance: Auditing shipment update - Shipment: {}, Status: {} -> {}",
                event.shipmentId(), event.oldStatus(), event.newStatus());

        try {
            // TODO: Create audit record for significant status changes
            if ("DELIVERED".equals(event.newStatus()) || "CANCELLED".equals(event.newStatus())) {
                createAuditLog(
                        "SHIPMENT_" + event.newStatus(),
                        event.orderId(),
                        null,
                        String.format("Shipment %d %s via %s (Tracking: %s)",
                                event.shipmentId(), event.newStatus().toLowerCase(),
                                event.carrier(), event.trackingNumber()));
            }

        } catch (Exception e) {
            log.error("Governance: Failed to audit shipment update {} - {}",
                    event.shipmentId(), e.getMessage(), e);
        }
    }

    // Helper methods (to be implemented with actual services)

    private void createAuditLog(String action, Long entityId, Long userId, String details) {
        log.info("AUDIT: {} | Entity: {} | User: {} | Details: {}",
                action, entityId, userId, details);
        // TODO: Persist to audit log table
    }

    private void flagForReview(PaymentCompletedEvent event) {
        log.warn("COMPLIANCE: Flagging order {} for manual review - High value transaction",
                event.orderId());
        // TODO: Create compliance review ticket
    }

    private void checkForFraud(PaymentFailedEvent event) {
        // TODO: Check failure patterns for potential fraud
        log.debug("FRAUD_CHECK: Analyzing payment failure pattern for user {}", event.userId());
    }

    private void trackCancellationReason(String reason) {
        log.debug("BUSINESS_INTEL: Tracking cancellation reason: {}", reason);
        // TODO: Aggregate cancellation reasons for reporting
    }
}
