package com.app.finance.pricing.modules;

import com.app.finance.pricing.OrderTotalModule;
import com.app.finance.pricing.contracts.OrderSummary;
import com.app.finance.pricing.contracts.OrderTotal;
import com.app.finance.pricing.contracts.OrderTotalInput;
import com.app.finance.promo.PromotionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DiscountModule implements OrderTotalModule {

    private final PromotionService promotionService;

    public DiscountModule(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

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
            BigDecimal discount = promotionService.applyCoupon(
                    input.getCouponCode(),
                    BigDecimal.valueOf(summary.getSubTotal()),
                    input.getEmail());

            if (discount.compareTo(BigDecimal.ZERO) < 0) {
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
