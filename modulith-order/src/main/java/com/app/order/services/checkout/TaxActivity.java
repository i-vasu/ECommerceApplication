package com.app.order.services.checkout;

import com.app.order.entities.Cart;
import com.app.identity.entities.Address;
import com.app.shipping.TaxCalculationService;
import com.app.shipping.TaxCalculationService.TaxCalculation;
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
        return taxCalculationService.calculateGST(cart, state);
    }
}
