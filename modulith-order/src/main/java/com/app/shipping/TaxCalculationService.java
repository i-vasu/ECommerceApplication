package com.app.shipping;

import com.app.order.entities.Cart;

public interface TaxCalculationService {
    TaxCalculation calculateGST(Cart cart, String state);

    public record TaxCalculation(double totalAmount, java.util.List<TaxComponent> components) {
    }

    public record TaxComponent(String name, double rate, double amount) {
    }
}
