package com.app.order.user;

import java.util.List;

import com.app.identity.entities.Address;
import com.app.order.payloads.AddressDTO;

public interface AddressService {

	AddressDTO createAddress(AddressDTO addressDTO);

	List<AddressDTO> getAddresses();

	AddressDTO getAddress(Long addressId);

	AddressDTO updateAddress(Long addressId, Address address);

	String deleteAddress(Long addressId);
}
