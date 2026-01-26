package com.app.commerce.promotion;

import java.math.BigDecimal;

/**
 * Service to calculate discounts based on codes or rules.
 */
public interface PromotionService {
    
    /**
     * @param code Coupon Code (e.g. SAVE10)
     * @param subTotal Current Cart Subtotal
     * @param email User Email
     * @return Discount Amount (Negative Value) or ZERO
     */
    BigDecimal applyCoupon(String code, BigDecimal subTotal, String email);
}
