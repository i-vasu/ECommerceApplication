package com.app.security.payloads;

import java.util.Set;
import java.time.LocalDate;
import java.util.Map;
import com.app.security.entities.Role;

public record UserDTO(
                Long userId,
                String firstName,
                String lastName,
                String mobileNumber,
                String email,
                String password,
                Set<Role> roles,
                AddressDTO address,
                String avatarUrl,
                LocalDate dateOfBirth,
                String gender,
                Map<String, String> preferences,
                String accountStatus,
                Integer rewardPoints,
                boolean isVerified,
                Set<String> savedPaymentMethods) {

    public static UserDTOBuilder builder() {
        return new UserDTOBuilder();
    }

    public UserDTOBuilder toBuilder() {
        return new UserDTOBuilder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .mobileNumber(mobileNumber)
                .email(email)
                .password(password)
                .roles(roles)
                .address(address)
                .avatarUrl(avatarUrl)
                .dateOfBirth(dateOfBirth)
                .gender(gender)
                .preferences(preferences)
                .accountStatus(accountStatus)
                .rewardPoints(rewardPoints)
                .isVerified(isVerified)
                .savedPaymentMethods(savedPaymentMethods);
    }

    public static class UserDTOBuilder {
        private Long userId;
        private String firstName;
        private String lastName;
        private String mobileNumber;
        private String email;
        private String password;
        private Set<Role> roles;
        private AddressDTO address;
        private String avatarUrl;
        private LocalDate dateOfBirth;
        private String gender;
        private Map<String, String> preferences;
        private String accountStatus;
        private Integer rewardPoints;
        private boolean isVerified;
        private Set<String> savedPaymentMethods;

        public UserDTOBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public UserDTOBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserDTOBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserDTOBuilder mobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public UserDTOBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserDTOBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserDTOBuilder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        public UserDTOBuilder address(AddressDTO address) {
            this.address = address;
            return this;
        }

        public UserDTOBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public UserDTOBuilder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public UserDTOBuilder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public UserDTOBuilder preferences(Map<String, String> preferences) {
            this.preferences = preferences;
            return this;
        }

        public UserDTOBuilder accountStatus(String accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public UserDTOBuilder rewardPoints(Integer rewardPoints) {
            this.rewardPoints = rewardPoints;
            return this;
        }

        public UserDTOBuilder isVerified(boolean isVerified) {
            this.isVerified = isVerified;
            return this;
        }

        public UserDTOBuilder savedPaymentMethods(Set<String> savedPaymentMethods) {
            this.savedPaymentMethods = savedPaymentMethods;
            return this;
        }

        public UserDTO build() {
            return new UserDTO(userId, firstName, lastName, mobileNumber, email, password, roles, address, avatarUrl, 
                    dateOfBirth, gender, preferences, accountStatus, rewardPoints, isVerified, savedPaymentMethods);
        }
    }
}
