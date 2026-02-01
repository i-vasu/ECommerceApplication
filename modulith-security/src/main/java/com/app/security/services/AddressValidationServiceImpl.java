package com.app.security.services;

import com.app.security.AddressValidationService;
import com.app.security.entities.Address;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AddressValidationServiceImpl implements AddressValidationService {

    private static final Pattern PINCODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern PO_BOX_PATTERN = Pattern.compile("(?i)\\b(p\\.?\\s*o\\.?\\s*box)\\b");

    @Override
    public AddressValidation validateIndianAddress(Address address) {
        if (address == null) return new AddressValidation(false, "Address is null");

        if (address.getPincode() == null || !PINCODE_PATTERN.matcher(address.getPincode()).matches()) {
            return new AddressValidation(false, "Invalid PIN code: " + address.getPincode());
        }

        if (address.getStreet() != null && PO_BOX_PATTERN.matcher(address.getStreet()).find()) {
            return new AddressValidation(false, "PO Box addresses are not supported.");
        }
        
        if (address.getBuildingName() != null && PO_BOX_PATTERN.matcher(address.getBuildingName()).find()) {
            return new AddressValidation(false, "PO Box addresses are not supported.");
        }

        if (address.getStreet() == null || address.getStreet().isBlank()) return new AddressValidation(false, "Street is mandatory");
        if (address.getCity() == null || address.getCity().isBlank()) return new AddressValidation(false, "City is mandatory");
        if (address.getState() == null || address.getState().isBlank()) return new AddressValidation(false, "State is mandatory");
        
        if (address.getCountry() != null && !"India".equalsIgnoreCase(address.getCountry()) && !"IN".equalsIgnoreCase(address.getCountry())) {
             return new AddressValidation(false, "Only India shipping supported");
        }

        return new AddressValidation(true, "Valid");
    }
}
