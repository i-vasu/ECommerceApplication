package com.app.identity.services;

import com.app.identity.UserService;
import com.app.core.APIException;

import com.app.cart.CartService;
import com.app.marketing.services.EmailService;
import com.app.order.services.ERPNextService;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import java.time.LocalDateTime;

import com.app.identity.mappers.IdentityMapper;
import com.app.cart.mappers.CartMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.product.config.AppConstants;
import com.app.identity.entities.Address;
import com.app.order.entites.Cart;
import com.app.order.entites.CartItem;
import com.app.identity.entities.Role;
import com.app.identity.entities.User;
import com.app.core.ResourceNotFoundException;
import com.app.order.payloads.AddressDTO;
import com.app.order.payloads.CartDTO;
import com.app.product.payloads.ProductDTO;
import com.app.order.payloads.UserDTO;
import com.app.order.payloads.UserResponse;
import com.app.identity.repositories.AddressRepo;
import com.app.identity.repositories.RoleRepo;
import com.app.identity.repositories.UserRepo;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private RoleRepo roleRepo;

	@Autowired
	private AddressRepo addressRepo;

	@Autowired
	private CartService cartService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private IdentityMapper identityMapper;

	@Autowired
	private CartMapper cartMapper;

	@Autowired
	private EmailService emailService;

	@Autowired
	private ERPNextService erpNextService;

	@Override
	public UserDTO registerUser(UserDTO userDTO) {

		try {
			User user = identityMapper.userDTOToUser(userDTO);

			// Generate verification code
			user.setVerificationCode(UUID.randomUUID().toString());
			user.setVerified(false);

			// Email sending
			emailService.sendSimpleMessage(user.getEmail(), "Email Verification",
					"Your verification code is: " + user.getVerificationCode());

			Cart cart = new Cart();
			user.setCart(cart);

			Role role = roleRepo.findById(AppConstants.USER_ID).get();
			user.getRoles().add(role);

			String country = userDTO.getAddress().getCountry();
			String state = userDTO.getAddress().getState();
			String city = userDTO.getAddress().getCity();
			String pincode = userDTO.getAddress().getPincode();
			String street = userDTO.getAddress().getStreet();
			String buildingName = userDTO.getAddress().getBuildingName();

			Address address = addressRepo.findByCountryAndStateAndCityAndPincodeAndStreetAndBuildingName(country, state,
					city, pincode, street, buildingName);

			if (address == null) {
				address = new Address(country, state, city, pincode, street, buildingName);

				address = addressRepo.save(address);
			}

			user.setAddresses(List.of(address));

			User registeredUser = userRepo.save(user);
			erpNextService.createCustomer(registeredUser);

			cart.setUser(registeredUser);

			userDTO = identityMapper.userToUserDTO(registeredUser);

			userDTO.setAddress(identityMapper.addressToAddressDTO(user.getAddresses().stream().findFirst().get()));

			return userDTO;
		} catch (DataIntegrityViolationException e) {
			throw new APIException("User already exists with emailId: " + userDTO.getEmail());
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
			UserDTO dto = identityMapper.userToUserDTO(user);

			if (user.getAddresses().size() != 0) {
				dto.setAddress(identityMapper.addressToAddressDTO(user.getAddresses().stream().findFirst().get()));
			}

			CartDTO cart = cartMapper.cartToCartDTO(user.getCart());

			List<ProductDTO> products = user.getCart().getCartItems().stream()
					.map(item -> {
						ProductDTO productDto = new ProductDTO();
						productDto.setProductId(item.getProductId());
						productDto.setProductName(item.getProductName());
						productDto.setPrice(item.getProductPrice());
						// Fetch more details if needed via client
						return productDto;
					}).collect(Collectors.toList());

			dto.setCart(cart);

			dto.getCart().setProducts(products);

			return dto;

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

		UserDTO userDTO = identityMapper.userToUserDTO(user);

		userDTO.setAddress(identityMapper.addressToAddressDTO(user.getAddresses().stream().findFirst().get()));

		CartDTO cart = cartMapper.cartToCartDTO(user.getCart());

		List<ProductDTO> products = user.getCart().getCartItems().stream()
				.map(item -> {
					ProductDTO productDto = new ProductDTO();
					productDto.setProductId(item.getProductId());
					productDto.setProductName(item.getProductName());
					productDto.setPrice(item.getProductPrice());
					return productDto;
				}).collect(Collectors.toList());

		userDTO.setCart(cart);

		userDTO.getCart().setProducts(products);

		return userDTO;
	}

	@Override
	public UserDTO updateUser(Long userId, UserDTO userDTO) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

		String encodedPass = passwordEncoder.encode(userDTO.getPassword());

		user.setFirstName(userDTO.getFirstName());
		user.setLastName(userDTO.getLastName());
		user.setMobileNumber(userDTO.getMobileNumber());
		user.setEmail(userDTO.getEmail());
		user.setPassword(encodedPass);

		if (userDTO.getAddress() != null) {
			String country = userDTO.getAddress().getCountry();
			String state = userDTO.getAddress().getState();
			String city = userDTO.getAddress().getCity();
			String pincode = userDTO.getAddress().getPincode();
			String street = userDTO.getAddress().getStreet();
			String buildingName = userDTO.getAddress().getBuildingName();

			Address address = addressRepo.findByCountryAndStateAndCityAndPincodeAndStreetAndBuildingName(country, state,
					city, pincode, street, buildingName);

			if (address == null) {
				address = new Address(country, state, city, pincode, street, buildingName);

				address = addressRepo.save(address);

				user.setAddresses(List.of(address));
			}
		}

		userDTO = identityMapper.userToUserDTO(user);

		userDTO.setAddress(identityMapper.addressToAddressDTO(user.getAddresses().stream().findFirst().get()));

		CartDTO cart = cartMapper.cartToCartDTO(user.getCart());

		List<ProductDTO> products = user.getCart().getCartItems().stream()
				.map(item -> {
					ProductDTO productDto = new ProductDTO();
					productDto.setProductId(item.getProductId());
					productDto.setProductName(item.getProductName());
					productDto.setPrice(item.getProductPrice());
					return productDto;
				}).collect(Collectors.toList());

		userDTO.setCart(cart);

		userDTO.getCart().setProducts(products);

		return userDTO;
	}

	@Override
	public String deleteUser(Long userId) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

		List<CartItem> cartItems = user.getCart().getCartItems();
		Long cartId = user.getCart().getCartId();

		cartItems.forEach(item -> {

			Long productId = item.getProductId();

			cartService.deleteProductFromCart(cartId, productId);
		});

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
			user.setVerified(true);
			user.setVerificationCode(null);
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

}
