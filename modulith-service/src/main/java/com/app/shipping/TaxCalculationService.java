package com.app.shipping;

import com.app.order.entites.Cart;

public interface TaxCalculationService {
    TaxCalculation calculateGST(Cart cart, String state);

    public record TaxCalculation(double amount, double rate, String gstType) {
    }
}
