package com.app.finance.pricing.modules;

import com.app.finance.pricing.OrderTotalModule;
import com.app.finance.pricing.contracts.OrderSummary;
import com.app.finance.pricing.contracts.OrderTotal;
import com.app.finance.pricing.contracts.OrderTotalInput;
import com.app.finance.tax.TaxCalculationService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DefaultTaxModule implements OrderTotalModule {

    private final TaxCalculationService taxService;

    public DefaultTaxModule(TaxCalculationService taxService) {
        this.taxService = taxService;
    }

    @Override
    public String getName() {
        return "tax";
    }

    @Override
    public int getSortOrder() {
        return 50; // Runs after Subtotal/Shipping
    }

    @Override
    public boolean isCritical() {
        return true;
    }

    @Override
    public OrderTotal calculate(OrderSummary summary, OrderTotalInput input) {
        BigDecimal currentTotal = summary.getFinalTotal();

        String state = input.getShippingState() != null ? input.getShippingState() : "Maharashtra";
        
        // Use item-level tax calculation if items are present
        var result = (input.getItems() != null && !input.getItems().isEmpty()) 
                ? taxService.calculateTax(input.getItems(), state)
                : taxService.calculateGST(currentTotal, state);

        return OrderTotal.builder()
                .code("tax")
                .title("Taxes")
                .value(result.totalAmount())
                .sortOrder(50)
                .build();
    }
}
