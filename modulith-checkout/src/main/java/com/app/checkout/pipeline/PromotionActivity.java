package com.app.checkout.pipeline;

import com.app.core.contracts.CartContract;
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
    public BigDecimal execute(CartContract cart, Address address) {
        if (cart.couponCode() == null || cart.couponCode().isBlank()) {
            return BigDecimal.ZERO;
        }
        var user = userRepo.findById(cart.userId()).orElse(null);
        String email = (user != null) ? user.getEmail() : null;

        return promotionService.applyCoupon(cart.couponCode(), cart.subTotal(),
                email);
    }
}
