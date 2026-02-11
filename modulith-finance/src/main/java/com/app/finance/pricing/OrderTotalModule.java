package com.app.finance.pricing;

import com.app.finance.pricing.contracts.OrderSummary;
import com.app.finance.pricing.contracts.OrderTotal;
import com.app.finance.pricing.contracts.OrderTotalInput;

/**
 * Strategy Interface for calculating a portion of the Order Total.
 * Examples: SubTotal, Tax, Shipping, Discount.
 */
public interface OrderTotalModule {

    /**
     * Unique name of the module (e.g., "tax", "shipping")
     */
    String getName();

    /**
     * Lower runs first.
     * 0-10: Subtotal, Discounts
     * 20-40: Shipping
     * 50-70: Tax
     * 100+: Final Total
     */
    int getSortOrder();

    /**
     * Calculate and return a Total line item.
     * Can assume previous totals are already in summary.
     */
    /**
     * If true, failure in this module will fail the entire order calculation.
     */
    default boolean isCritical() {
        return false;
    }

    OrderTotal calculate(OrderSummary summary, OrderTotalInput input);
}
