package com.app.commerce.pricing.modules;

import com.app.commerce.pricing.OrderTotalModule;
import com.app.commerce.pricing.contracts.OrderSummary;
import com.app.commerce.pricing.contracts.OrderTotal;
import com.app.commerce.pricing.contracts.OrderTotalInput;
import com.app.commerce.promotion.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DiscountModule implements OrderTotalModule {

    private final PromotionService promotionService;

    @Override
    public String getName() {
        return "discount";
    }

    @Override
    public int getSortOrder() {
        return 20; // After Subtotal, Before Shipping/Tax
    }

    @Override
    public OrderTotal calculate(OrderSummary summary, OrderTotalInput input) {
        // Need to pass coupon code in Input! For now, let's assume it might come from Cart metadata 
        // or just hardcode checking a known context (not available yet in Input).
        // For PoC, we skip active lookup unless input has it.
        // TODO: Add 'couponCode' to OrderTotalInput
        
        return null; 
    }
}
