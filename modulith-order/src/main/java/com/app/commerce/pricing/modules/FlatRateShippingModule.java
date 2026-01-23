package com.app.commerce.pricing.modules;

import com.app.commerce.pricing.OrderTotalModule;
import com.app.commerce.pricing.contracts.OrderSummary;
import com.app.commerce.pricing.contracts.OrderTotal;
import com.app.commerce.pricing.contracts.OrderTotalInput;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FlatRateShippingModule implements OrderTotalModule {

    @Override
    public String getName() {
        return "shipping";
    }

    @Override
    public int getSortOrder() {
        return 30; // Runs after Subtotal, before Tax
    }

    @Override
    public OrderTotal calculate(OrderSummary summary, OrderTotalInput input) {
        BigDecimal subTotal = summary.getFinalTotal();
        
        // Free shipping over 500
        BigDecimal shippingCost;
        if (subTotal.compareTo(BigDecimal.valueOf(500)) > 0) {
            shippingCost = BigDecimal.ZERO;
        } else {
            shippingCost = BigDecimal.valueOf(50);
        }

        return OrderTotal.builder()
                .code("shipping")
                .title("Flat Rate Shipping")
                .value(shippingCost)
                .sortOrder(30)
                .build();
    }
}
