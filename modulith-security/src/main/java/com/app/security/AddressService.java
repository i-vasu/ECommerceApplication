package com.app.security;

import com.app.security.entities.Address;
import com.app.security.payloads.AddressDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddressService {

	AddressDTO createAddress(AddressDTO addressDTO);

	Page<AddressDTO> getAddresses(Pageable pageable);

	AddressDTO getAddress(Long addressId);

	AddressDTO updateAddress(Long addressId, Address address);

	String deleteAddress(Long addressId);
}
