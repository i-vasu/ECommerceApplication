package com.app.order.services.checkout;

import com.app.order.entities.Cart;
import com.app.identity.entities.Address;
import com.app.identity.AddressValidationService;
import com.app.identity.AddressValidationService.AddressValidation;
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
    public AddressValidation execute(Cart cart, Address address) {
        if (address == null) {
            return new AddressValidation(false, "Address is missing");
        }
        return addressValidationService.validateIndianAddress(address);
    }
}
