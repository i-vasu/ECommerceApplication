package com.app.finance.payloads;

import java.math.BigDecimal;

public record VendorDTO(
    Long id,
    String name,
    String email,
    String phoneNumber,
    String gstin,
    String pan,
    String category,
    AddressDTO address,
    BigDecimal commissionRate,
    String bankName,
    String bankAccountNumber,
    String ifscCode,
    String paymentTerms,
    boolean active
) {
    public record AddressDTO(
        String street,
        String city,
        String state,
        String pincode,
        String country
    ) {}
}
