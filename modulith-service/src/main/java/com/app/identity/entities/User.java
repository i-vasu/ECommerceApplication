package com.app.identity.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import jakarta.validation.constraints.Email;
import com.app.order.entites.Cart;
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
	@Column(name = "mobile_number")
	private String mobileNumber;

	@Email
	@Column(unique = true, nullable = false)
	private String email;

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

	@OneToOne(mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = true)
	private Cart cart;

	@Column(name = "verification_code")
	private String verificationCode;

	@Column(name = "is_verified", nullable = false)
	private boolean isVerified = false;

	@Column(name = "reset_token")
	private String resetToken;

	@Column(name = "reset_token_expiry")
	private java.time.LocalDateTime resetTokenExpiry;

	public User() {
	}

	public User(Long userId, String firstName, String lastName, String mobileNumber, String email, String password,
			Set<Role> roles, List<Address> addresses, Integer rewardPoints, Cart cart, String verificationCode,
			boolean isVerified, String resetToken, java.time.LocalDateTime resetTokenExpiry) {
		this.userId = userId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.mobileNumber = mobileNumber;
		this.email = email;
		this.password = password;
		this.roles = roles;
		this.addresses = addresses;
		this.rewardPoints = rewardPoints;
		this.cart = cart;
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

	public Cart getCart() {
		return cart;
	}

	public void setCart(Cart cart) {
		this.cart = cart;
	}

	public String getVerificationCode() {
		return verificationCode;
	}

	public void setVerificationCode(String verificationCode) {
		this.verificationCode = verificationCode;
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

	public java.time.LocalDateTime getResetTokenExpiry() {
		return resetTokenExpiry;
	}

	public void setResetTokenExpiry(java.time.LocalDateTime resetTokenExpiry) {
		this.resetTokenExpiry = resetTokenExpiry;
	}
}
