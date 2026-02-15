package com.app.security.services;

import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.core.multitenancy.UserContext;
import com.app.core.async.EventProducer;
import com.app.security.UserService;
import com.app.security.async.UserEvent;
import com.app.security.entities.Address;
import com.app.security.entities.User;
import com.app.security.entities.UserLoyalty;
import com.app.security.entities.UserProfile;
import com.app.security.mappers.IdentityMapper;
import com.app.security.payloads.AddressDTO;
import com.app.security.payloads.UserDTO;
import com.app.security.payloads.UserResponse;
import com.app.security.repositories.AddressRepo;
import com.app.security.repositories.UserRepo;
import com.app.security.repositories.UserFriendRepo;
import com.app.security.entities.UserFriend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@Service
public class UserServiceImpl implements UserService, com.app.core.contracts.UserServiceContract {

	private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(UserServiceImpl.class);

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private AddressRepo addressRepo;

	@Autowired
	private UserFriendRepo userFriendRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private IdentityMapper identityMapper;

	@Autowired
	private com.app.core.services.RedisLockService lockService;

	@Autowired
	private com.app.governance.states.OperationalStateMachineService stateMachineService;

	@Autowired
	private com.app.governance.rules.RuleEngineService ruleEngine;

	@Autowired
	private EventProducer eventProducer;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Override
	public UserDTO registerUser(UserDTO userDTO) {
		String lockKey = "register:" + userDTO.email();
		boolean locked = lockService.tryLock(lockKey, java.time.Duration.ofMinutes(1));
		if (!locked) {
			throw new APIException("Registration is already in progress for this email.");
		}

		try {
			// Password Complexity Validation (MANDATORY for Fashion PoC)
			String rawPassword = userDTO.password();
			if (rawPassword != null) {
				String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
				if (!rawPassword.matches(pattern)) {
					throw new APIException(
							"Password must be 8+ chars, including Uppercase, Lowercase, Digit, and Special character.");
				}
			}

			User user = identityMapper.userDTOToUser(userDTO);

			// Ensure password is encoded (Defense in depth)
			if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
				user.setPassword(passwordEncoder.encode(user.getPassword()));
			}

			user.setVerificationCode(UUID.randomUUID().toString());
			user.setVerificationCodeExpiry(LocalDateTime.now().plusHours(24));
			user.setVerified(false);
			user.setAccountStatus("PENDING_VERIFICATION");

			// Create Profile
			UserProfile profile = new UserProfile();
			profile.setUser(user);
			profile.setFirstName(userDTO.firstName());
			profile.setLastName(userDTO.lastName());
			profile.setMobileNumber(userDTO.mobileNumber());
			user.setProfile(profile);

			// Create Loyalty
			UserLoyalty loyalty = new UserLoyalty();
			loyalty.setUser(user);
			loyalty.setCustomerGroup("RETAIL");
			loyalty.setRewardPoints(0);
			user.setLoyalty(loyalty);
			
			User registeredUser = userRepo.save(user);
			
			// Formally register with the State Machine
			stateMachineService.triggerAccountEvent(registeredUser.getUserId(), com.app.governance.states.AccountEvent.VERIFY);

			// ASYNC WRITE-BEHIND & DOMAIN EVENT
			try {
				String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
				
				// 1. Module-Specific Event (Legacy/Internal)
				UserEvent event = new UserEvent(
						registeredUser.getUserId(), "USER_REGISTERED", tenantId);
				eventProducer.publish("user_events", event);

				// 2. Domain-Wide Kernel Event (Inter-module)
				eventPublisher.publishEvent(new com.app.core.events.UserRegisteredEvent(
						registeredUser.getUserId(),
						registeredUser.getEmail(),
						registeredUser.getProfile().getFirstName(),
						registeredUser.getProfile().getLastName()
				));

			} catch (Exception e) {
				log.error("Failed to queue user registered events for {}: {}", registeredUser.getEmail(), e.getMessage());
			}

			AddressDTO addressDTO = null;
			if (user.getProfile().getAddresses() != null && !user.getProfile().getAddresses().isEmpty()) {
				addressDTO = identityMapper.addressToAddressDTO(user.getProfile().getAddresses().getFirst());
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
		} finally {
			lockService.unlock(lockKey);
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
			if (user.getProfile() != null && user.getProfile().getAddresses() != null && !user.getProfile().getAddresses().isEmpty()) {
				addressDTO = identityMapper.addressToAddressDTO(user.getProfile().getAddresses().getFirst());
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
		validateUserIdOwnership(userId);
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
		if (user.getProfile() != null && user.getProfile().getAddresses() != null && !user.getProfile().getAddresses().isEmpty()) {
			addressDTO = identityMapper.addressToAddressDTO(user.getProfile().getAddresses().getFirst());
		}

		return baseUserDTO.toBuilder()
				.address(addressDTO)
				.build();
	}

	@Override
	public UserDTO updateUser(Long userId, UserDTO userDTO) {
		validateUserIdOwnership(userId);
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

		String encodedPass = passwordEncoder.encode(userDTO.password());

		user.getProfile().setFirstName(userDTO.firstName());
		user.getProfile().setLastName(userDTO.lastName());
		user.getProfile().setMobileNumber(userDTO.mobileNumber());
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

				user.getProfile().setAddresses(List.of(address));
			}
		}

		// Use mapper to create base UserDTO
		UserDTO baseUserDTO = identityMapper.userToUserDTO(user);

		// Get address DTO if available
		AddressDTO addressDTO = null;
		if (user.getProfile() != null && user.getProfile().getAddresses() != null && !user.getProfile().getAddresses().isEmpty()) {
			addressDTO = identityMapper.addressToAddressDTO(user.getProfile().getAddresses().get(0));
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
			user.setAccountStatus("ACTIVE");
			user.setVerificationCode(null);
			user.setVerificationCodeExpiry(null);
			
			// Transition to ACTIVE state
			stateMachineService.triggerAccountEvent(user.getUserId(), com.app.governance.states.AccountEvent.VERIFY);
			
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

		// DECOUPLED: Publish event for notification system
		eventPublisher.publishEvent(new com.app.core.events.ForgotPasswordEvent(email, token));
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
		validateUserIdOwnership(senderId);
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

		int senderBalance = sender.getLoyalty().getRewardPoints() != null ? sender.getLoyalty().getRewardPoints() : 0;
		if (senderBalance < points) {
			throw new APIException("Insufficient reward points");
		}

		int receiverBalance = receiver.getLoyalty().getRewardPoints() != null ? receiver.getLoyalty().getRewardPoints() : 0;

		sender.getLoyalty().setRewardPoints(senderBalance - points);
		receiver.getLoyalty().setRewardPoints(receiverBalance + points);

		userRepo.save(sender);
		userRepo.save(receiver);
	}

	@Override
	public List<UserDTO> getFriends(Long userId) {
		validateUserIdOwnership(userId);
		
		var friendships = userFriendRepo.findAcceptedFriendsByUserId(userId);
		
		return friendships.stream()
				.map(uf -> {
					User friend = uf.getFriend();
					return mapUserToDTO(friend);
				})
				.toList();
	}

	@Override
	public void addFriend(Long userId, String friendEmail) {
		validateUserIdOwnership(userId);
		
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
		
		User friend = userRepo.findByEmail(friendEmail)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", friendEmail));
		
		if (user.getUserId().equals(friend.getUserId())) {
			throw new APIException("Cannot add yourself as a friend");
		}
		
		// Check if friendship already exists
		var existing = userFriendRepo.findByUserIdAndFriendId(userId, friend.getUserId());
		if (existing.isPresent()) {
			throw new APIException("Friend request already exists");
		}
		
		// Create bidirectional friendship (auto-accept for now, can be changed to pending)
		UserFriend friendship = new UserFriend();
		friendship.setUser(user);
		friendship.setFriend(friend);
		friendship.setStatus(UserFriend.FriendshipStatus.ACCEPTED);
		friendship.setAcceptedAt(LocalDateTime.now());
		userFriendRepo.save(friendship);
		
		// Create reverse friendship for bidirectional lookup
		UserFriend reverseFriendship = new UserFriend();
		reverseFriendship.setUser(friend);
		reverseFriendship.setFriend(user);
		reverseFriendship.setStatus(UserFriend.FriendshipStatus.ACCEPTED);
		reverseFriendship.setAcceptedAt(LocalDateTime.now());
		userFriendRepo.save(reverseFriendship);
		
		log.info("Friendship created between User {} and User {}", userId, friend.getUserId());
	}

	@Override
	public void deactivateAccount(Long userId) {
		validateUserIdOwnership(userId);
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
		user.setAccountStatus("CLOSED");
		
		// Trigger Formal Lifecycle Closure
		stateMachineService.triggerAccountEvent(userId, com.app.governance.states.AccountEvent.CLOSE);
		
		userRepo.save(user);
	}

	// UserServiceContract implementations
	@Override
	public boolean userExists(Long userId) {
		return userRepo.existsById(userId);
	}

	@Override
	public boolean userExistsByEmail(String email) {
		return userRepo.findByEmail(email).isPresent();
	}

	@Override
	public String getUserEmail(Long userId) {
		return userRepo.findById(userId)
				.map(User::getEmail)
				.orElse(null);
	}

	@Override
	public String getUserFullName(Long userId) {
		return userRepo.findById(userId)
				.map(u -> u.getProfile().getFirstName() + " " + u.getProfile().getLastName())
				.orElse(null);
	}

	private void validateUserIdOwnership(Long userId) {
		Long currentUserId = UserContext.getCurrentUserId();
		if (currentUserId != null && !currentUserId.equals(userId)) {
			log.warn("IDOR attempt detected! Authenticated User {} tried to access/modify User {}", currentUserId, userId);
			throw new APIException("Unauthorized: You cannot access or modify another user's profile.");
		}
	}
}
