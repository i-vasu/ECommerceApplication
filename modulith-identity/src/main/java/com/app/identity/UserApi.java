package com.app.identity;

import com.app.identity.payloads.UserDTO;
import com.app.identity.payloads.UserResponse;
import com.app.core.payloads.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "User v1", description = "User Management APIs - Version 1")
public interface UserApi {

    @Operation(summary = "Get All Users", description = "Retrieves a paginated list of users (Admin only)")
    ResponseEntity<ApiResponse<UserResponse>> getUsers(Integer pageNumber, Integer pageSize, String sortBy,
            String sortOrder);

    @Operation(summary = "Get User by ID", description = "Retrieves user details by ID")
    ResponseEntity<ApiResponse<UserDTO>> getUser(Long userId);

    @Operation(summary = "Update User", description = "Updates user details")
    ResponseEntity<ApiResponse<UserDTO>> updateUser(UserDTO userDTO, Long userId);

    @Operation(summary = "Delete User", description = "Deletes a user by ID")
    ResponseEntity<ApiResponse<String>> deleteUser(Long userId);

    @Operation(summary = "Transfer Reward Points", description = "Transfers reward points from one user to another")
    ResponseEntity<ApiResponse<String>> transferRewardPoints(Long userId, String recipient, int points);

    @Operation(summary = "Get Friends", description = "Retrieves the user's friend list")
    ResponseEntity<ApiResponse<List<UserDTO>>> getFriends(Long userId);

    @Operation(summary = "Add Friend", description = "Adds a user to the friend list by email")
    ResponseEntity<ApiResponse<String>> addFriend(Long userId, String friendEmail);
}
