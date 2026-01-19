package com.app.order.user;

import java.util.List;

import com.app.identity.entities.Address;
import com.app.order.payloads.AddressDTO;

public interface AddressService {

	AddressDTO createAddress(AddressDTO addressDTO);

	org.springframework.data.domain.Page<AddressDTO> getAddresses(org.springframework.data.domain.Pageable pageable);

	AddressDTO getAddress(Long addressId);

	AddressDTO updateAddress(Long addressId, Address address);

	String deleteAddress(Long addressId);
}
