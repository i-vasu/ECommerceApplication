package com.app.user;

import com.app.payloads.UserDTO;
import com.app.payloads.UserResponse;

public interface UserService {
	UserDTO registerUser(UserDTO userDTO);

	UserResponse getAllUsers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

	UserDTO getUserById(Long userId);

	UserDTO updateUser(Long userId, UserDTO userDTO);

	String deleteUser(Long userId);

	void verifyEmail(String email, String code);

	void forgotPassword(String email);

	void resetPassword(String token, String newPassword);

	void transferRewardPoints(Long senderId, String recipient, int points);
}
