package com.app.checkout.pipeline;

import com.app.core.contracts.CartContract;
import com.app.security.AddressValidationService;
import com.app.security.AddressValidationService.AddressValidation;
import com.app.security.entities.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddressValidationActivity implements CheckoutActivity<AddressValidation> {

    private final AddressValidationService addressValidationService;

    @Override
    public String getName() {
        return "address-validation";
    }

    @Override
    public AddressValidation execute(CartContract cart, Address address) {
        if (address == null) {
            return new AddressValidation(false, "Address is missing");
        }
        return addressValidationService.validateIndianAddress(address);
    }
}
