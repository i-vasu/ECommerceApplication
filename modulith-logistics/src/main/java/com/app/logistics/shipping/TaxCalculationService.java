package com.app.logistics.shipping;

public interface TaxCalculationService {
    TaxCalculation calculateGST(double subtotal, String state);

    public record TaxCalculation(double totalAmount, java.util.List<TaxComponent> components) {
    }

    public record TaxComponent(String name, double rate, double amount) {
    }
}
