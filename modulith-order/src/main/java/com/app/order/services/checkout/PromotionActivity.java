package com.app.order.services.checkout;

import com.app.order.entities.Cart;
import com.app.identity.entities.Address;
import com.app.commerce.promotion.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PromotionActivity implements CheckoutActivity<BigDecimal> {

    private final PromotionService promotionService;

    @Override
    public String getName() {
        return "promotion-evaluation";
    }

    @Override
    public BigDecimal execute(Cart cart, Address address) {
        if (cart.getCouponCode() == null || cart.getCouponCode().isBlank()) {
            return BigDecimal.ZERO;
        }
        return promotionService.applyCoupon(cart.getCouponCode(), BigDecimal.valueOf(cart.getTotalPrice()),
                cart.getEmail());
    }
}
