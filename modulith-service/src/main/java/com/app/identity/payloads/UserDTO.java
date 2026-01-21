package com.app.identity.payloads;

import java.util.Set;
import java.time.LocalDate;
import java.util.Map;
import com.app.identity.entities.Role;
import lombok.Builder;
import com.app.cart.payloads.CartDTO;

@Builder(toBuilder = true)
public record UserDTO(
                Long userId,
                String firstName,
                String lastName,
                String mobileNumber,
                String email,
                String password,
                Set<Role> roles,
                AddressDTO address,
                CartDTO cart,
                String avatarUrl,
                LocalDate dateOfBirth,
                String gender,
                Map<String, String> preferences,
                String accountStatus,
                Integer rewardPoints,
                boolean isVerified,
                Set<String> savedPaymentMethods) {
}
