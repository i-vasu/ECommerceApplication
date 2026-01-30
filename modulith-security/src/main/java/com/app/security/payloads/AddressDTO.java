package com.app.security.payloads;

public record AddressDTO(
        Long addressId,
        String street,
        String buildingName,
        String city,
        String state,
        String country,
        String pincode,
        boolean isDefaultShipping,
        boolean isDefaultBilling,
        String label,
        String receiverPhoneNumber) {
}
