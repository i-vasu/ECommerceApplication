package com.app.security;

import com.app.security.payloads.UserDTO;
import com.app.security.payloads.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "User v1", description = "User Profile and Relationship Management APIs")
public interface UserApi {

    @Operation(summary = "Paginated User List", description = "Retrieves a paginated list of all registered users. Restricted to ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    ResponseEntity<com.app.core.payloads.ApiResponse<UserResponse>> getUsers(
            @Parameter(description = "Page number (0-indexed)") Integer pageNumber,
            @Parameter(description = "Items per page") Integer pageSize,
            @Parameter(description = "Field to sort by") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") String sortOrder);

    @Operation(summary = "My Profile", description = "Retrieves the profile of the currently authenticated user.")
    @ApiResponse(responseCode = "200", description = "Current user profile retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT missing or invalid")
    ResponseEntity<com.app.core.payloads.ApiResponse<UserDTO>> getUserProfileMe();

    @Operation(summary = "User Details", description = "Retrieves details of a specific user. Restricted to the user themselves or an ADMIN.")
    @ApiResponse(responseCode = "200", description = "User details found")
    @ApiResponse(responseCode = "403", description = "Forbidden - Cannot access other users' data")
    @ApiResponse(responseCode = "404", description = "User not found")
    ResponseEntity<com.app.core.payloads.ApiResponse<UserDTO>> getUser(
            @Parameter(description = "Target user ID") Long userId);

    @Operation(summary = "Update Profile", description = "Updates profile information for a specific user.")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    ResponseEntity<com.app.core.payloads.ApiResponse<UserDTO>> updateUser(
            @Parameter(description = "Updated user data") UserDTO userDTO,
            @Parameter(description = "ID of the user to update") Long userId);

    @Operation(summary = "Deactivate/Delete User", description = "Permanently removes a user account. Restricted to ADMIN.")
    @ApiResponse(responseCode = "200", description = "User deleted successfully")
    ResponseEntity<com.app.core.payloads.ApiResponse<String>> deleteUser(
            @Parameter(description = "ID of the user to delete") Long userId);

    @Operation(summary = "Transfer Rewards", description = "Transfers reward points to another user's account using their email.")
    @ApiResponse(responseCode = "200", description = "Transfer successful")
    @ApiResponse(responseCode = "400", description = "Insufficient points or user not found")
    ResponseEntity<com.app.core.payloads.ApiResponse<String>> transferRewardPoints(
            @Parameter(description = "Source user ID") Long userId,
            @Parameter(description = "Recipient user email") String recipient,
            @Parameter(description = "Points to transfer") int points);

    @Operation(summary = "List Friends", description = "Retrieves a list of friends for the specified user.")
    @ApiResponse(responseCode = "200", description = "Friends list retrieved")
    ResponseEntity<com.app.core.payloads.ApiResponse<List<UserDTO>>> getFriends(
            @Parameter(description = "User ID") Long userId);

    @Operation(summary = "Add Friend", description = "Adds a new friend using their email address.")
    @ApiResponse(responseCode = "200", description = "Friend added successfully")
    ResponseEntity<com.app.core.payloads.ApiResponse<String>> addFriend(
            @Parameter(description = "Your user ID") Long userId,
            @Parameter(description = "Friend's email address") String friendEmail);
}
