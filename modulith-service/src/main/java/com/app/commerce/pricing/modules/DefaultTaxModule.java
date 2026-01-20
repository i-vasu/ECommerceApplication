package com.app.commerce.pricing.modules;

import com.app.commerce.pricing.OrderTotalModule;
import com.app.commerce.pricing.contracts.OrderSummary;
import com.app.commerce.pricing.contracts.OrderTotal;
import com.app.commerce.pricing.contracts.OrderTotalInput;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DefaultTaxModule implements OrderTotalModule {

    @Override
    public String getName() {
        return "tax";
    }

    @Override
    public int getSortOrder() {
        return 50; // Runs after Subtotal/Shipping
    }

    @Override
    public OrderTotal calculate(OrderSummary summary, OrderTotalInput input) {
        BigDecimal currentTotal = summary.getFinalTotal();
        // Simple Logic: 18% GST on everything
        BigDecimal tax = currentTotal.multiply(BigDecimal.valueOf(0.18));

        return OrderTotal.builder()
                .code("tax")
                .title("GST (18%)")
                .value(tax)
                .sortOrder(50)
                .build();
    }
}
