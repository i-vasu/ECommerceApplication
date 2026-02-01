package com.app.security.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	@jakarta.persistence.Version
	private Long version;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private UserProfile profile;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private UserLoyalty loyalty;

	@Email
	@Column(unique = true, nullable = false)
	private String email;

	@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", message = "Password must be at least 8 characters long and contain at least one digit, one lowercase, one uppercase, and one special character")
	private String password;

	@ManyToMany(cascade = { CascadeType.MERGE }, fetch = FetchType.EAGER)
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();





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



	@ElementCollection
	@CollectionTable(name = "user_payment_methods", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "vault_token")
	private Set<String> savedPaymentMethods = new HashSet<>();

	@Column(name = "account_status")
	private String accountStatus = "ACTIVE"; // ACTIVE, DEACTIVATED

	public User() {
	}

	public User(Long userId, String email, String password, Set<Role> roles, String verificationCode,
			boolean isVerified, String resetToken, LocalDateTime resetTokenExpiry) {
		this.userId = userId;
		this.email = email;
		this.password = password;
		this.roles = roles;
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



	public UserProfile getProfile() {
		return profile;
	}

	public void setProfile(UserProfile profile) {
		this.profile = profile;
	}

	public UserLoyalty getLoyalty() {
		return loyalty;
	}

	public void setLoyalty(UserLoyalty loyalty) {
		this.loyalty = loyalty;
	}
}
