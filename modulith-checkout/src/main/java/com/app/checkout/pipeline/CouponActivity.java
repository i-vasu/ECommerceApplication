package com.app.checkout.pipeline;

import com.app.cart.entities.Cart;
import com.app.finance.promo.CouponValidationService;
import com.app.finance.promo.CouponValidationService.CouponDiscount;
import com.app.security.entities.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponActivity implements CheckoutActivity<CouponDiscount> {

    private final CouponValidationService couponValidationService;

    @Override
    public String getName() {
        return "coupon-validation";
    }

    @Override
    public CouponDiscount execute(Cart cart, Address address) {
        String couponCode = cart.getCouponCode();
        if (couponCode == null || couponCode.isBlank()) {
            return new CouponDiscount(0.0, couponCode);
        }
        return couponValidationService.validateAndCalculate(couponCode, cart.getTotalPrice().doubleValue());
    }
}
