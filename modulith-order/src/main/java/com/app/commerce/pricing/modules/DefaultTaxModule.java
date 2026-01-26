package com.app.commerce.pricing.modules;

import com.app.commerce.pricing.OrderTotalModule;
import com.app.commerce.pricing.contracts.OrderSummary;
import com.app.commerce.pricing.contracts.OrderTotal;
import com.app.commerce.pricing.contracts.OrderTotalInput;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@lombok.RequiredArgsConstructor
public class DefaultTaxModule implements OrderTotalModule {

    private final com.app.shipping.TaxCalculationService taxService;

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

        // Convert summary to a mock cart for the service
        com.app.order.entities.Cart mockCart = new com.app.order.entities.Cart();
        mockCart.setTotalPrice(currentTotal.doubleValue());

        var result = taxService.calculateGST(mockCart, "Default");

        return OrderTotal.builder()
                .code("tax")
                .title("Taxes")
                .value(BigDecimal.valueOf(result.totalAmount()))
                .sortOrder(50)
                .build();
    }
}
