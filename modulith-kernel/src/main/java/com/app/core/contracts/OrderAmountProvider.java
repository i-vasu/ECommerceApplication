package com.app.core.contracts;

import java.math.BigDecimal;
import java.util.Optional;

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
            String currentStatus) {
    }
}
