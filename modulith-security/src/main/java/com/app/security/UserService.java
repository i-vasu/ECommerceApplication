package com.app.security;

import java.util.List;

import com.app.security.payloads.UserDTO;
import com.app.security.payloads.UserResponse;

public interface UserService {
	UserDTO registerUser(UserDTO userDTO);

	UserResponse getAllUsers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

	UserDTO getUserById(Long userId);

	UserDTO getUserByEmail(String email);

	UserDTO updateUser(Long userId, UserDTO userDTO);

	String deleteUser(Long userId);

	void verifyEmail(String email, String code);

	void forgotPassword(String email);

	void resetPassword(String token, String newPassword);

	void transferRewardPoints(Long senderId, String recipient, int points);

	List<UserDTO> getFriends(Long userId);

	void addFriend(Long userId, String friendEmail);

	void deactivateAccount(Long userId);
}
