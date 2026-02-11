package com.app.logistics.shipping;

public interface ShippingCalculationService {
    ShippingCost calculateCost(String pincode);

    public record ShippingCost(java.math.BigDecimal amount, String carrier, int estimatedDays) {
    }
}
