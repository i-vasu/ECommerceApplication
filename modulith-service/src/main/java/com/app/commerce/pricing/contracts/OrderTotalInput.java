package com.app.commerce.pricing.contracts;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Context required by Modules to calculate Totals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTotalInput {
    // Current Cart/Order ID
    private Long id;

    // User Email (for personalized discounts)
    private String email;

    // Address (for Tax/Shipping)
    private String shippingCountry;
    private String shippingState;
    private String shippingZip;

    // Currency
    private String currencyCode;

    // Coupon Code
    private String couponCode;
}
