package com.app.security.mappers;

import com.app.security.entities.Address;
import com.app.security.entities.User;
import com.app.security.payloads.AddressDTO;
import com.app.security.payloads.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IdentityMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(source = "profile.firstName", target = "firstName")
    @Mapping(source = "profile.lastName", target = "lastName")
    @Mapping(source = "profile.mobileNumber", target = "mobileNumber")
    @Mapping(source = "profile.avatarUrl", target = "avatarUrl")
    @Mapping(source = "profile.dateOfBirth", target = "dateOfBirth")
    @Mapping(source = "profile.gender", target = "gender")
    @Mapping(source = "profile.preferences", target = "preferences")
    @Mapping(source = "loyalty.rewardPoints", target = "rewardPoints")
    @Mapping(source = "profile.addresses", target = "address", qualifiedByName = "mapFirstAddress")
    UserDTO userToUserDTO(User user);

    @org.mapstruct.Named("mapFirstAddress")
    default AddressDTO mapFirstAddress(java.util.List<Address> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        return addressToAddressDTO(addresses.get(0));
    }

    User userDTOToUser(UserDTO userDTO);

    AddressDTO addressToAddressDTO(Address address);

    Address addressDTOToAddress(AddressDTO addressDTO);
}
