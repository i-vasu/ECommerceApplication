package com.app.identity;

import com.app.identity.entities.Address;

public interface AddressValidationService {
    AddressValidation validateIndianAddress(Address address);

    public record AddressValidation(boolean valid, String message) {
    }
}
