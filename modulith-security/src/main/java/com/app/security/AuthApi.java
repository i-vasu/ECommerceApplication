package com.app.security;

import com.app.security.payloads.LoginCredentials;
import com.app.security.payloads.TokenRefreshRequest;
import com.app.security.payloads.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Tag(name = "Auth", description = "Authentication & Authorization APIs")
public interface AuthApi {

    @Operation(summary = "Register User", description = "Registers a new user")
    ResponseEntity<Map<String, Object>> registerHandler(UserDTO user);

    @Operation(summary = "Login User", description = "Authenticates a user and returns a token")
    Map<String, Object> loginHandler(LoginCredentials credentials);

    @Operation(summary = "Refresh Token", description = "Refreshes the authentication token")
    ResponseEntity<?> refreshtoken(TokenRefreshRequest request);

    @Operation(summary = "Verify Email", description = "Verifies user email using a code")
    ResponseEntity<String> verifyEmail(String email, String code);

    @Operation(summary = "Forgot Password", description = "Initiates password recovery process")
    ResponseEntity<String> forgotPassword(String email);

    @Operation(summary = "Reset Password", description = "Resets user password using a token")
    ResponseEntity<String> resetPassword(String token, String newPassword);
}
