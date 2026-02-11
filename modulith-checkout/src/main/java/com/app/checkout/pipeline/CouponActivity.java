package com.app.checkout.pipeline;

import com.app.core.contracts.CartContract;
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
    public CouponDiscount execute(CartContract cart, Address address) {
        String couponCode = cart.couponCode();
        if (couponCode == null || couponCode.isBlank()) {
            return new CouponDiscount(java.math.BigDecimal.ZERO, couponCode);
        }
        return couponValidationService.validateAndCalculate(couponCode, cart.subTotal());
    }
}
