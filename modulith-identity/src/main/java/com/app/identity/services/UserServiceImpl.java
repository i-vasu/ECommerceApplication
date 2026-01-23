package com.app.identity.services;

import com.app.identity.async.UserEvent;
import lombok.extern.log4j.Log4j2;

import com.app.identity.UserService;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;

import com.app.marketing.services.EmailService;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.UUID;
import java.time.LocalDateTime;

import com.app.identity.mappers.IdentityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.identity.entities.Address;
import com.app.identity.entities.User;
import com.app.identity.payloads.AddressDTO;
import com.app.identity.payloads.UserDTO;
import com.app.identity.payloads.UserResponse;
import com.app.identity.repositories.AddressRepo;
import com.app.identity.repositories.RoleRepo;
import com.app.identity.repositories.UserRepo;

@Log4j2
@Transactional
@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private AddressRepo addressRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private IdentityMapper identityMapper;

	@Autowired
	private EmailService emailService;

	@Autowired
	private com.app.core.async.EventProducer eventProducer;

	@Override
	public UserDTO registerUser(UserDTO userDTO) {
		// Password Complexity Validation (MANDATORY for Fashion PoC)
		String rawPassword = userDTO.password();
		if (rawPassword != null) {
			String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
			if (!rawPassword.matches(pattern)) {
				throw new APIException(
						"Password must be 8+ chars, including Uppercase, Lowercase, Digit, and Special character.");
			}
		}

		try {
			User user = identityMapper.userDTOToUser(userDTO);

			// Ensure password is encoded (Defense in depth)
			if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
				user.setPassword(passwordEncoder.encode(user.getPassword()));
			}

			user.setVerificationCode(UUID.randomUUID().toString());
			user.setVerificationCodeExpiry(LocalDateTime.now().plusHours(24));
			user.setVerified(false);

			User registeredUser = userRepo.save(user);

			// ASYNC WRITE-BEHIND
			try {
				String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
				UserEvent event = new UserEvent(
						registeredUser.getUserId(), "USER_REGISTERED", tenantId);
				eventProducer.publish("user_events", event);
			} catch (Exception e) {
				log.error("Failed to queue user sync for {}: {}", registeredUser.getEmail(), e.getMessage());
			}

			AddressDTO addressDTO = null;
			if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
				addressDTO = identityMapper.addressToAddressDTO(user.getAddresses().getFirst());
			}

			userDTO = identityMapper.userToUserDTO(registeredUser).toBuilder()
					.address(addressDTO)
					.build();

			return userDTO;
		} catch (DataIntegrityViolationException e) {
			if (e.getMessage() != null && e.getMessage().contains("mobile_number")) {
				throw new APIException("User already exists with mobile number: " + userDTO.mobileNumber());
			}
			throw new APIException("User already exists with emailId: " + userDTO.email());
		}

	}

	@Override
	public UserResponse getAllUsers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
		Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

		Page<User> pageUsers = userRepo.findAll(pageDetails);

		List<User> users = pageUsers.getContent();

		if (users.size() == 0) {
			throw new APIException("No User exists !!!");
		}

		List<UserDTO> userDTOs = users.stream().map(user -> {
			UserDTO baseDTO = identityMapper.userToUserDTO(user);

			AddressDTO addressDTO = null;
			if (user.getAddresses() != null && user.getAddresses().size() != 0) {
				addressDTO = identityMapper.addressToAddressDTO(user.getAddresses().getFirst());
			}

			return baseDTO.toBuilder()
					.address(addressDTO)
					.build();

		}).collect(Collectors.toList());

		UserResponse userResponse = new UserResponse();

		userResponse.setContent(userDTOs);
		userResponse.setPageNumber(pageUsers.getNumber());
		userResponse.setPageSize(pageUsers.getSize());
		userResponse.setTotalElements(pageUsers.getTotalElements());
		userResponse.setTotalPages(pageUsers.getTotalPages());
		userResponse.setLastPage(pageUsers.isLast());

		return userResponse;
	}

	@Override
	public UserDTO getUserById(Long userId) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

		return mapUserToDTO(user);
	}

	@Override
	public UserDTO getUserByEmail(String email) {
		User user = userRepo.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

		return mapUserToDTO(user);
	}

	private UserDTO mapUserToDTO(User user) {
		UserDTO baseUserDTO = identityMapper.userToUserDTO(user);

		AddressDTO addressDTO = null;
		if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
			addressDTO = identityMapper.addressToAddressDTO(user.getAddresses().getFirst());
		}

		return baseUserDTO.toBuilder()
				.address(addressDTO)
				.build();
	}

	@Override
	public UserDTO updateUser(Long userId, UserDTO userDTO) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

		String encodedPass = passwordEncoder.encode(userDTO.password());

		user.setFirstName(userDTO.firstName());
		user.setLastName(userDTO.lastName());
		user.setMobileNumber(userDTO.mobileNumber());
		user.setEmail(userDTO.email());
		user.setPassword(encodedPass);

		if (userDTO.address() != null) {
			String country = userDTO.address().country();
			String state = userDTO.address().state();
			String city = userDTO.address().city();
			String pincode = userDTO.address().pincode();
			String street = userDTO.address().street();
			String buildingName = userDTO.address().buildingName();

			Address address = addressRepo.findByCountryAndStateAndCityAndPincodeAndStreetAndBuildingName(country, state,
					city, pincode, street, buildingName);

			if (address == null) {
				address = new Address(country, state, city, pincode, street, buildingName);

				address = addressRepo.save(address);

				user.setAddresses(List.of(address));
			}
		}

		// Use mapper to create base UserDTO
		UserDTO baseUserDTO = identityMapper.userToUserDTO(user);

		// Get address DTO if available
		AddressDTO addressDTO = null;
		if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
			addressDTO = identityMapper.addressToAddressDTO(user.getAddresses().get(0));
		}

		// Build new UserDTO with all fields using the builder
		return baseUserDTO.toBuilder()
				.address(addressDTO)
				.build();
	}

	@Override
	public String deleteUser(Long userId) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

		userRepo.delete(user);

		return "User with userId " + userId + " deleted successfully!!!";
	}

	@Override
	public void verifyEmail(String email, String code) {
		User user = userRepo.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

		if (user.isVerified()) {
			throw new APIException("User is already verified");
		}

		if (code.equals(user.getVerificationCode())) {
			if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
				throw new APIException("Verification code has expired. Please request a new one.");
			}
			user.setVerified(true);
			user.setVerificationCode(null);
			user.setVerificationCodeExpiry(null);
			userRepo.save(user);
		} else {
			throw new APIException("Invalid verification code");
		}
	}

	@Override
	public void forgotPassword(String email) {
		User user = userRepo.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

		String token = UUID.randomUUID().toString();
		user.setResetToken(token);
		user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
		userRepo.save(user);

		// Email sending
		emailService.sendSimpleMessage(email, "Password Reset", "Your password reset token is: " + token);
	}

	@Override
	public void resetPassword(String token, String newPassword) {
		User user = userRepo.findByResetToken(token)
				.orElseThrow(() -> new APIException("Invalid password reset token"));

		if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
			throw new APIException("Token expired");
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		user.setResetToken(null);
		user.setResetTokenExpiry(null);
		userRepo.save(user);
	}

	@Override
	public void transferRewardPoints(Long senderId, String recipient, int points) {
		if (points <= 0) {
			throw new APIException("Points to transfer must be positive");
		}

		User sender = userRepo.findById(senderId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", senderId));

		User receiver;
		if (recipient.contains("@")) {
			receiver = userRepo.findByEmail(recipient)
					.orElseThrow(() -> new ResourceNotFoundException("User", "email", recipient));
		} else {
			// Assume mobile number
			receiver = userRepo.findByMobileNumber(recipient)
					.orElseThrow(() -> new ResourceNotFoundException("User", "mobileNumber", recipient));
		}

		if (sender.getUserId().equals(receiver.getUserId())) {
			throw new APIException("Cannot transfer points to yourself");
		}

		int senderBalance = sender.getRewardPoints() != null ? sender.getRewardPoints() : 0;
		if (senderBalance < points) {
			throw new APIException("Insufficient reward points");
		}

		int receiverBalance = receiver.getRewardPoints() != null ? receiver.getRewardPoints() : 0;

		sender.setRewardPoints(senderBalance - points);
		receiver.setRewardPoints(receiverBalance + points);

		userRepo.save(sender);
		userRepo.save(receiver);
	}

	@Override
	public List<UserDTO> getFriends(Long userId) {
		// Basic stub for now to resolve compilation
		return new ArrayList<>();
	}

	@Override
	public void addFriend(Long userId, String friendEmail) {
		// Basic stub for now to resolve compilation
	}

	@Override
	public void deactivateAccount(Long userId) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
		user.setAccountStatus("DEACTIVATED");
		userRepo.save(user);
	}
}
