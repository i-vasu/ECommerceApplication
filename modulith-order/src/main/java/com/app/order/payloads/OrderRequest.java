package com.app.order.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private Long addressId;
    private String couponCode;
    
    // Optional: Fields for a new address if not using saved addressId
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private String receiverPhoneNumber;
}
