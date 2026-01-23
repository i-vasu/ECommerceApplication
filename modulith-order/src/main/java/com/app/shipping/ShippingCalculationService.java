package com.app.shipping;

public interface ShippingCalculationService {
    ShippingCost calculateCost(String pincode);

    public record ShippingCost(double amount, String carrier, int estimatedDays) {
    }
}
