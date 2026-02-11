package com.app.core.contracts;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.util.Map;

/**
 * Contract to allow other modules to retrieve core order data without
 * depending on the full Order entity or modulith-order module.
 */
public interface OrderAmountProvider {
    Optional<OrderSummary> getOrderSummary(Long orderId);

    record OrderSummary(
            Long orderId,
            String email,
            BigDecimal totalAmount,
            String currentStatus,
            BigDecimal taxAmount,             // Added for Invoice
            Map<String, BigDecimal> taxBreakdown, // CGST, SGST, etc.
            List<OrderItemSummary> items      // Snapshot of items
            ) {
    }

    record OrderItemSummary(
            String itemCode,
            String name,
            Integer quantity,
            BigDecimal price,
            BigDecimal taxAmount
    ) {}
}
