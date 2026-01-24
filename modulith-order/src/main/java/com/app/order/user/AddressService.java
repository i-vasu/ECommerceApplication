package com.app.order.user;

import com.app.identity.entities.Address;
import com.app.identity.payloads.AddressDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddressService {

	AddressDTO createAddress(AddressDTO addressDTO);

	Page<AddressDTO> getAddresses(Pageable pageable);

	AddressDTO getAddress(Long addressId);

	AddressDTO updateAddress(Long addressId, Address address);

	String deleteAddress(Long addressId);
}
