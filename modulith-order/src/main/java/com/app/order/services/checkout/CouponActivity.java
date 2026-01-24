package com.app.order.services.checkout;

import com.app.order.entities.Cart;
import com.app.identity.entities.Address;
import com.app.discount.CouponValidationService;
import com.app.discount.CouponValidationService.CouponDiscount;
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
        return couponValidationService.validateAndCalculate(couponCode, cart.getTotalPrice());
    }
}
