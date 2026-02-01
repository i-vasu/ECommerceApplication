package com.app.checkout.pipeline;

import com.app.cart.entities.Cart;
import com.app.finance.promo.PromotionService;
import com.app.security.entities.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PromotionActivity implements CheckoutActivity<BigDecimal> {

    private final PromotionService promotionService;
    private final com.app.security.repositories.UserRepo userRepo;

    @Override
    public String getName() {
        return "promotion-evaluation";
    }

    @Override
    public BigDecimal execute(Cart cart, Address address) {
        if (cart.getCouponCode() == null || cart.getCouponCode().isBlank()) {
            return BigDecimal.ZERO;
        }
        var user = userRepo.findById(cart.getUserId()).orElse(null);
        String email = (user != null) ? user.getEmail() : null;

        return promotionService.applyCoupon(cart.getCouponCode(), cart.getTotalPrice(),
                email);
    }
}
