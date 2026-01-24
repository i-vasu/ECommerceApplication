package com.app.identity.mappers;

import com.app.identity.entities.Address;
import com.app.identity.entities.User;
import com.app.identity.payloads.AddressDTO;
import com.app.identity.payloads.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IdentityMapper {

    @Mapping(target = "password", ignore = true)
    UserDTO userToUserDTO(User user);

    User userDTOToUser(UserDTO userDTO);

    AddressDTO addressToAddressDTO(Address address);

    Address addressDTOToAddress(AddressDTO addressDTO);
}
