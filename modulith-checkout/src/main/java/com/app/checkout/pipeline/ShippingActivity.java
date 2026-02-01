package com.app.checkout.pipeline;

import com.app.cart.entities.Cart;
import com.app.logistics.shipping.ShippingCalculationService;
import com.app.logistics.shipping.ShippingCalculationService.ShippingCost;
import com.app.security.entities.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShippingActivity implements CheckoutActivity<ShippingCost> {

    private final ShippingCalculationService shippingCalculationService;

    @Override
    public String getName() {
        return "shipping-calculation";
    }

    @Override
    public ShippingCost execute(Cart cart, Address address) {
        String pincode = (address != null) ? address.getPincode() : "000000";
        return shippingCalculationService.calculateCost(pincode);
    }
}
