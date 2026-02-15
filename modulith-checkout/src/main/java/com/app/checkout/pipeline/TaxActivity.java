package com.app.checkout.pipeline;

import com.app.core.contracts.CartContract;
import com.app.finance.tax.TaxCalculationService;
import com.app.finance.tax.TaxCalculationService.TaxCalculation;
import com.app.security.entities.Address;
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
    public TaxCalculation execute(CartContract cart, Address address) {
        String state = (address != null) ? address.getState() : "Default";
        return taxCalculationService.calculateGST(cart.subTotal(), state);
    }
}
