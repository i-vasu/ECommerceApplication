package com.app.security;

import com.app.security.entities.Address;
import com.app.security.payloads.AddressDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class AddressController implements AddressApi {

	private final AddressService addressService;

	@PostMapping("/address")
	@Override
	public ResponseEntity<AddressDTO> createAddress(@Valid @RequestBody AddressDTO addressDTO) {
		var savedAddressDTO = addressService.createAddress(addressDTO);
		return new ResponseEntity<>(savedAddressDTO, HttpStatus.CREATED);
	}

	@GetMapping("/addresses")
	@Override
	public ResponseEntity<Page<AddressDTO>> getAddresses(Pageable pageable) {
		var addressDTOs = addressService.getAddresses(pageable);
		return new ResponseEntity<>(addressDTOs, HttpStatus.OK);
	}

	@GetMapping("/addresses/{addressId}")
	@Override
	public ResponseEntity<AddressDTO> getAddress(@PathVariable Long addressId) {
		var addressDTO = addressService.getAddress(addressId);
		return new ResponseEntity<>(addressDTO, HttpStatus.FOUND);
	}

	@PutMapping("/addresses/{addressId}")
	@Override
	public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long addressId, @RequestBody Address address) {
		var addressDTO = addressService.updateAddress(addressId, address);
		return new ResponseEntity<>(addressDTO, HttpStatus.OK);
	}

	@DeleteMapping("/addresses/{addressId}")
	@Override
	public ResponseEntity<String> deleteAddress(@PathVariable Long addressId) {
		var status = addressService.deleteAddress(addressId);
		return new ResponseEntity<>(status, HttpStatus.OK);
	}
}
