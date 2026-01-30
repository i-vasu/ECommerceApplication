package com.app.security;

import com.app.security.entities.Address;

public interface AddressValidationService {
    AddressValidation validateIndianAddress(Address address);

    public record AddressValidation(boolean valid, String message) {
    }
}
