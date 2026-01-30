package com.app.finance.pricing.modules;

import com.app.finance.pricing.OrderTotalModule;
import com.app.finance.pricing.contracts.OrderSummary;
import com.app.finance.pricing.contracts.OrderTotal;
import com.app.finance.pricing.contracts.OrderTotalInput;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SubTotalModule implements OrderTotalModule {

    @Override
    public String getName() {
        return "subtotal";
    }

    @Override
    public int getSortOrder() {
        return 10; // Runs First
    }

    @Override
    public OrderTotal calculate(OrderSummary summary, OrderTotalInput input) {
        if (input.getItems() == null || input.getItems().isEmpty()) {
            return null;
        }

        BigDecimal subTotal = BigDecimal.ZERO;
        for (OrderTotalInput.ItemInput item : input.getItems()) {
            BigDecimal price = BigDecimal.valueOf(item.getPrice() != null ? item.getPrice() : 0.0);
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 0);
            subTotal = subTotal.add(price.multiply(qty));
        }

        return OrderTotal.builder()
                .code("subtotal")
                .title("Sub Total")
                .value(subTotal)
                .sortOrder(10)
                .build();
    }
}
