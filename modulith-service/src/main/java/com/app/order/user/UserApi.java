package com.app.order.user;

import com.app.order.payloads.UserDTO;
import com.app.order.payloads.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "User", description = "User Management APIs")
public interface UserApi {

    @Operation(summary = "Get All Users", description = "Retrieves a paginated list of users (Admin only)")
    ResponseEntity<UserResponse> getUsers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    @Operation(summary = "Get User by ID", description = "Retrieves user details by ID")
    ResponseEntity<UserDTO> getUser(Long userId);

    @Operation(summary = "Update User", description = "Updates user details")
    ResponseEntity<UserDTO> updateUser(UserDTO userDTO, Long userId);

    @Operation(summary = "Delete User", description = "Deletes a user by ID")
    ResponseEntity<String> deleteUser(Long userId);

    @Operation(summary = "Transfer Reward Points", description = "Transfers reward points from one user to another")
    ResponseEntity<String> transferRewardPoints(Long userId, String recipient, int points);
}
