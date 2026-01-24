package com.app.identity;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
import org.springframework.web.bind.annotation.PostMapping;

import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import com.app.config.AppConstants;
import com.app.identity.payloads.UserDTO;
import com.app.identity.payloads.UserResponse;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Log4j2
@RestController
@RequestMapping("/api")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class UserController implements UserApi {

	private final UserService userService;

	@GetMapping("/admin/users")
	@Override
	public ResponseEntity<ApiResponse<UserResponse>> getUsers(
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USERS_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		var userResponse = userService.getAllUsers(pageNumber, pageSize, sortBy, sortOrder);

		return ResponseEntity.ok(ApiResponse.success(userResponse, "Users retrieved successfully"));
	}

	@GetMapping("/public/users/{userId}")
	@Override
	public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable Long userId) {
		var user = userService.getUserById(userId);

		return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
	}

	@PutMapping("/public/users/{userId}")
	@Override
	public ResponseEntity<ApiResponse<UserDTO>> updateUser(@RequestBody UserDTO userDTO, @PathVariable Long userId) {
		var updatedUser = userService.updateUser(userId, userDTO);

		return ResponseEntity.ok(ApiResponse.success(updatedUser, "User updated successfully"));
	}

	@DeleteMapping("/admin/users/{userId}")
	@Override
	public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long userId) {
		var status = userService.deleteUser(userId);

		return ResponseEntity.ok(ApiResponse.success(status, "User deleted successfully"));
	}

	@PostMapping("/public/users/{userId}/rewards/transfer")
	@Override
	public ResponseEntity<ApiResponse<String>> transferRewardPoints(@PathVariable Long userId,
			@RequestParam String recipient, @RequestParam int points) {
		userService.transferRewardPoints(userId, recipient, points);
		return ResponseEntity.ok(ApiResponse.success("Points transferred successfully", "Success"));
	}

	@GetMapping("/public/users/{userId}/friends")
	@Override
	public ResponseEntity<ApiResponse<List<UserDTO>>> getFriends(@PathVariable Long userId) {
		var friends = userService.getFriends(userId);
		return ResponseEntity.ok(ApiResponse.success(friends, "Friends retrieved successfully"));
	}

	@PostMapping("/public/users/{userId}/friends")
	@Override
	public ResponseEntity<ApiResponse<String>> addFriend(@PathVariable Long userId, @RequestParam String friendEmail) {
		userService.addFriend(userId, friendEmail);
		return ResponseEntity.ok(ApiResponse.success("Friend added successfully", "Success"));
	}
}
