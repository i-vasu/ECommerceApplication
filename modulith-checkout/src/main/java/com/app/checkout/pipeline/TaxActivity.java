package com.app.checkout.pipeline;

import com.app.cart.entities.Cart;
import com.app.security.entities.Address;
import com.app.logistics.shipping.TaxCalculationService;
import com.app.logistics.shipping.TaxCalculationService.TaxCalculation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaxActivity implements CheckoutActivity<TaxCalculation> {

    private final TaxCalculationService taxCalculationService;

    @Override
    public String getName() {
        return "tax-calculation";
    }

    @Override
    public TaxCalculation execute(Cart cart, Address address) {
        String state = (address != null) ? address.getState() : "Default";
        return taxCalculationService.calculateGST(cart.getTotalPrice().doubleValue(), state);
    }
}
