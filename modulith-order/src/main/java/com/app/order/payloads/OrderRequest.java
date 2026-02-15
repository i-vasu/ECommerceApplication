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

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getReceiverPhoneNumber() { return receiverPhoneNumber; }
    public void setReceiverPhoneNumber(String receiverPhoneNumber) { this.receiverPhoneNumber = receiverPhoneNumber; }
}
