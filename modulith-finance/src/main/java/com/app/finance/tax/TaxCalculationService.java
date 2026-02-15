package com.app.finance.tax;

import java.math.BigDecimal;
import java.util.List;

public interface TaxCalculationService {
    TaxCalculation calculateGST(BigDecimal subtotal, String state);

    TaxCalculation calculateTax(List<com.app.finance.pricing.contracts.OrderTotalInput.ItemInput> items, String state);

    public record TaxCalculation(BigDecimal totalAmount, List<TaxComponent> components) {
    }

    public record TaxComponent(String name, BigDecimal rate, BigDecimal amount) {
    }
}
