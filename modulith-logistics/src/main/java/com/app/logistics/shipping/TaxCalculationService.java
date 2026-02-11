package com.app.logistics.shipping;

public interface TaxCalculationService {
    TaxCalculation calculateGST(java.math.BigDecimal subtotal, String state);

    public record TaxCalculation(java.math.BigDecimal totalAmount, java.util.List<TaxComponent> components) {
    }

    public record TaxComponent(String name, java.math.BigDecimal rate, java.math.BigDecimal amount) {
    }
}
