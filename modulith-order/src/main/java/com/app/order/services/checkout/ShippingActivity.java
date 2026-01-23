package com.app.order.services.checkout;

import com.app.order.entities.Cart;
import com.app.identity.entities.Address;
import com.app.shipping.ShippingCalculationService;
import com.app.shipping.ShippingCalculationService.ShippingCost;
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
