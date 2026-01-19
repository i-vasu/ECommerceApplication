package com.app.order.user;

import com.app.identity.entities.Address;
import com.app.order.payloads.AddressDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Address", description = "Address Management APIs")
public interface AddressApi {

    @Operation(summary = "Create Address", description = "Creates a new address for the user")
    ResponseEntity<AddressDTO> createAddress(AddressDTO addressDTO);

    @Operation(summary = "Get All Addresses", description = "Retrieves all addresses (Admin/Paginated)")
    ResponseEntity<org.springframework.data.domain.Page<AddressDTO>> getAddresses(org.springframework.data.domain.Pageable pageable);

    @Operation(summary = "Get Address by ID", description = "Retrieves a specific address by ID")
    ResponseEntity<AddressDTO> getAddress(Long addressId);

    @Operation(summary = "Update Address", description = "Updates an existing address")
    ResponseEntity<AddressDTO> updateAddress(Long addressId, Address address);

    @Operation(summary = "Delete Address", description = "Deletes an address by ID")
    ResponseEntity<String> deleteAddress(Long addressId);
}
