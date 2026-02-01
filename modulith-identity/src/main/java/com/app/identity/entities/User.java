package com.app.identity.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.MapKeyColumn;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "app_users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

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

	@Email
	@Column(unique = true, nullable = false)
	private String email;

	@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", message = "Password must be at least 8 characters long and contain at least one digit, one lowercase, one uppercase, and one special character")
	private String password;

	@ManyToMany(cascade = { CascadeType.MERGE }, fetch = FetchType.EAGER)
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@ManyToMany(cascade = { CascadeType.MERGE })
	@JoinTable(name = "user_address", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "address_id"))
	private List<Address> addresses = new ArrayList<>();

	// Loyalty
	@Column(name = "reward_points")
	private Integer rewardPoints = 0;

	@Column(name = "customer_group")
	private String customerGroup = "RETAIL"; // Default to RETAIL

	@Column(name = "verification_code")
	private String verificationCode;

	@Column(name = "verification_code_expiry")
	private LocalDateTime verificationCodeExpiry;

	@Column(name = "is_verified", nullable = false)
	private boolean isVerified = false;

	@Column(name = "reset_token")
	private String resetToken;

	@Column(name = "reset_token_expiry")
	private LocalDateTime resetTokenExpiry;

	@Column(name = "avatar_url")
	private String avatarUrl;

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;

	private String gender;

	@ElementCollection
	@CollectionTable(name = "user_preferences", joinColumns = @JoinColumn(name = "user_id"))
	@MapKeyColumn(name = "pref_key")
	@Column(name = "pref_value")
	private Map<String, String> preferences = new HashMap<>();

	@ManyToMany
	@JoinTable(name = "user_friends", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "friend_id"))
	private Set<User> friends = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "user_payment_methods", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "vault_token")
	private Set<String> savedPaymentMethods = new HashSet<>();

	@Column(name = "account_status")
	private String accountStatus = "ACTIVE"; // ACTIVE, DEACTIVATED

	public User() {
	}

	public User(Long userId, String firstName, String lastName, String mobileNumber, String email, String password,
			Set<Role> roles, List<Address> addresses, Integer rewardPoints, String verificationCode,
			boolean isVerified, String resetToken, LocalDateTime resetTokenExpiry) {
		this.userId = userId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.mobileNumber = mobileNumber;
		this.email = email;
		this.password = password;
		this.roles = roles;
		this.addresses = addresses;
		this.rewardPoints = rewardPoints;
		this.verificationCode = verificationCode;
		this.isVerified = isVerified;
		this.resetToken = resetToken;
		this.resetTokenExpiry = resetTokenExpiry;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public List<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	public Integer getRewardPoints() {
		return rewardPoints;
	}

	public void setRewardPoints(Integer rewardPoints) {
		this.rewardPoints = rewardPoints;
	}

	public String getVerificationCode() {
		return verificationCode;
	}

	public void setVerificationCode(String verificationCode) {
		this.verificationCode = verificationCode;
	}

	public LocalDateTime getVerificationCodeExpiry() {
		return verificationCodeExpiry;
	}

	public void setVerificationCodeExpiry(LocalDateTime verificationCodeExpiry) {
		this.verificationCodeExpiry = verificationCodeExpiry;
	}

	public boolean isVerified() {
		return isVerified;
	}

	public void setVerified(boolean verified) {
		isVerified = verified;
	}

	public String getResetToken() {
		return resetToken;
	}

	public void setResetToken(String resetToken) {
		this.resetToken = resetToken;
	}

	public LocalDateTime getResetTokenExpiry() {
		return resetTokenExpiry;
	}

	public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
		this.resetTokenExpiry = resetTokenExpiry;
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

	public Set<String> getSavedPaymentMethods() {
		return savedPaymentMethods;
	}

	public void setSavedPaymentMethods(Set<String> savedPaymentMethods) {
		this.savedPaymentMethods = savedPaymentMethods;
	}

	public String getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(String accountStatus) {
		this.accountStatus = accountStatus;
	}

	public String getCustomerGroup() {
		return customerGroup;
	}

	public void setCustomerGroup(String customerGroup) {
		this.customerGroup = customerGroup;
	}

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "user_segment_map", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "segment_id"))
	private Set<com.app.identity.entities.CustomerSegment> segments = new HashSet<>();

	public Set<com.app.identity.entities.CustomerSegment> getSegments() {
		return segments;
	}

	public void setSegments(Set<com.app.identity.entities.CustomerSegment> segments) {
		this.segments = segments;
	}
}
