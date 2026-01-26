package com.app.identity.services;

import com.app.identity.entities.Address;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AddressValidationServiceTest {

    private final AddressValidationServiceImpl validationService = new AddressValidationServiceImpl();

    @Test
    void validateIndianAddress_Valid() {
        Address address = new Address();
        address.setStreet("123, MG Road");
        address.setCity("Bangalore");
        address.setState("Karnataka");
        address.setPincode("560001");
        address.setCountry("India");
        
        var result = validationService.validateIndianAddress(address);
        assertTrue(result.valid());
    }

    @Test
    void validateIndianAddress_InvalidPincode() {
        Address address = new Address();
        address.setStreet("Street");
        address.setCity("City");
        address.setState("State");
        address.setPincode("123"); // Too short
        
        var result = validationService.validateIndianAddress(address);
        assertFalse(result.valid());
        // Exact message validation might vary, check regex
        assertTrue(result.message().contains("Invalid PIN code"));
    }

    @Test
    void validateIndianAddress_POBox() {
        Address address = new Address();
        address.setStreet("P.O. Box 123");
        address.setCity("City");
        address.setState("State");
        address.setPincode("560001");
        
        var result = validationService.validateIndianAddress(address);
        assertFalse(result.valid());
        assertTrue(result.message().contains("PO Box"));
    }
    
    @Test
    void validateIndianAddress_MissingMandatory() {
        Address address = new Address();
        // Missing fields
        
        var result = validationService.validateIndianAddress(address);
        assertFalse(result.valid());
    }
}
