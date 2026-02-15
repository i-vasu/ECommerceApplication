package com.app.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    @JsonIgnore
    private User user;

    @Size(min = 5, max = 20, message = "First Name must be between 5 and 30 characters long")
    @Pattern(regexp = "^[a-zA-Z]*$", message = "First Name must not contain numbers or special characters")
    @Column(name = "first_name")
    private String firstName;

    @Size(min = 5, max = 20, message = "Last Name must be between 5 and 30 characters long")
    @Pattern(regexp = "^[a-zA-Z]*$", message = "Last Name must not contain numbers or special characters")
    @Column(name = "last_name")
    private String lastName;

    @Size(min = 10, max = 10, message = "Mobile Number must be exactly 10 digits long")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile Number must contain only Numbers")
    @Column(name = "mobile_number", unique = true)
    private String mobileNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    @ManyToMany(cascade = { CascadeType.MERGE })
    @JoinTable(name = "user_profile_address", joinColumns = @JoinColumn(name = "profile_id"), inverseJoinColumns = @JoinColumn(name = "address_id"))
    private List<Address> addresses = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_preferences", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "pref_key")
    @Column(name = "pref_value")
    private Map<String, String> preferences = new HashMap<>();

    @ManyToMany
    @JoinTable(name = "user_friends", joinColumns = @JoinColumn(name = "profile_id"), inverseJoinColumns = @JoinColumn(name = "friend_user_id"))
    private Set<User> friends = new HashSet<>();

    // Manual Getters and Setters
    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public Map<String, String> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, String> preferences) {
        this.preferences = preferences;
    }

    public Set<User> getFriends() {
        return friends;
    }

    public void setFriends(Set<User> friends) {
        this.friends = friends;
    }
}
