package com.app.commerce.pricing.modules;

import com.app.commerce.pricing.OrderTotalModule;
import com.app.commerce.pricing.contracts.OrderSummary;
import com.app.commerce.pricing.contracts.OrderTotal;
import com.app.commerce.pricing.contracts.OrderTotalInput;
import com.app.commerce.promotion.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        if (input.getCouponCode() != null && !input.getCouponCode().isEmpty()) {
            java.math.BigDecimal discount = promotionService.applyCoupon(
                    input.getCouponCode(),
                    java.math.BigDecimal.valueOf(summary.getSubTotal()),
                    input.getEmail());

            if (discount.compareTo(java.math.BigDecimal.ZERO) < 0) {
                return OrderTotal.builder()
                        .code("discount")
                        .title("Discount")
                        .value(discount)
                        .sortOrder(20)
                        .build();
            }
        }

        return null;
    }
}
