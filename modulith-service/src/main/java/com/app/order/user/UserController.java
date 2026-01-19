package com.app.order.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.product.config.AppConstants;
import com.app.order.payloads.UserDTO;
import com.app.order.payloads.UserResponse;
import com.app.identity.UserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "E-Commerce Application")
public class UserController implements UserApi {

	@Autowired
	private UserService userService;

	@GetMapping("/admin/users")
	@Override
	public ResponseEntity<UserResponse> getUsers(
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USERS_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		UserResponse userResponse = userService.getAllUsers(pageNumber, pageSize, sortBy, sortOrder);

		return new ResponseEntity<UserResponse>(userResponse, HttpStatus.FOUND);
	}

	@GetMapping("/public/users/{userId}")
	@Override
	public ResponseEntity<UserDTO> getUser(@PathVariable Long userId) {
		UserDTO user = userService.getUserById(userId);

		return new ResponseEntity<UserDTO>(user, HttpStatus.FOUND);
	}

	@PutMapping("/public/users/{userId}")
	@Override
	public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO userDTO, @PathVariable Long userId) {
		UserDTO updatedUser = userService.updateUser(userId, userDTO);

		return new ResponseEntity<UserDTO>(updatedUser, HttpStatus.OK);
	}

	@DeleteMapping("/admin/users/{userId}")
	@Override
	public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
		String status = userService.deleteUser(userId);

		return new ResponseEntity<String>(status, HttpStatus.OK);
	}

	@org.springframework.web.bind.annotation.PostMapping("/public/users/{userId}/rewards/transfer")
	@Override
	public ResponseEntity<String> transferRewardPoints(@PathVariable Long userId,
			@RequestParam String recipient, @RequestParam int points) {
		userService.transferRewardPoints(userId, recipient, points);
		return new ResponseEntity<>("Points transferred successfully", HttpStatus.OK);
	}
}
