package com.app.security;

import com.app.core.APIException;
import com.app.core.security.JWTUtil;
import com.app.security.entities.RefreshToken;
import com.app.security.payloads.LoginCredentials;
import com.app.security.payloads.TokenRefreshRequest;
import com.app.security.payloads.TokenRefreshResponse;
import com.app.security.payloads.UserDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "E-Commerce Application")
public class AuthController implements AuthApi {

	private final UserService userService;
	private final JWTUtil jwtUtil;
	private final AuthenticationManager authenticationManager;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;

	public AuthController(UserService userService, JWTUtil jwtUtil, AuthenticationManager authenticationManager, 
                          PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService) {
		this.userService = userService;
		this.jwtUtil = jwtUtil;
		this.authenticationManager = authenticationManager;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
	}

	@PostMapping("/register")
	@Override
	public ResponseEntity<Map<String, Object>> registerHandler(@Valid @RequestBody UserDTO user) {
		String encodedPass = passwordEncoder.encode(user.password());

		UserDTO userWithEncodedPass = user.toBuilder().password(encodedPass).build();

		UserDTO userDTO = userService.registerUser(userWithEncodedPass);

		String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
		String token = jwtUtil.generateToken(userDTO.email(), tenantId);

		return new ResponseEntity<Map<String, Object>>(Collections.singletonMap("jwt-token", token),
				HttpStatus.CREATED);
	}

	@PostMapping("/login")
	@Override
	public Map<String, Object> loginHandler(@Valid @RequestBody LoginCredentials credentials) {

		UsernamePasswordAuthenticationToken authCredentials = new UsernamePasswordAuthenticationToken(
				credentials.email(), credentials.password());

		authenticationManager.authenticate(authCredentials);

		String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
		String token = jwtUtil.generateToken(credentials.email(), tenantId);

		UserDTO user = userService.getUserByEmail(credentials.email());

		if (!user.isVerified()) {
			throw new APIException("Email is not verified. Please verify your email.");
		}

		RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.userId());

		Map<String, Object> response = new HashMap<>();
		response.put("jwt-token", token);
		response.put("refresh-token", refreshToken.getToken());
		return response;
	}

	@PostMapping("/refresh-token")
	@Override
	public ResponseEntity<?> refreshtoken(@Valid @RequestBody TokenRefreshRequest request) {
		String requestRefreshToken = request.getRefreshToken();

		return refreshTokenService.findByToken(requestRefreshToken)
				.map(refreshTokenService::verifyExpiration)
				.map(RefreshToken::getUser)
				.map(user -> {
					String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
					String token = jwtUtil.generateToken(user.getEmail(), tenantId);
					return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
				})
				.orElseThrow(() -> new APIException("Refresh token is not in database!"));
	}

	@PostMapping("/verify-email")
	@Override
	public ResponseEntity<String> verifyEmail(@RequestParam String email, @RequestParam String code) {
		userService.verifyEmail(email, code);
		return ResponseEntity.ok("Email verified successfully");
	}

	@PostMapping("/forgot-password")
	@Override
	public ResponseEntity<String> forgotPassword(@RequestParam String email) {
		userService.forgotPassword(email);
		return ResponseEntity.ok("Password reset token sent to email");
	}

	@PostMapping("/reset-password")
	@Override
	public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
		userService.resetPassword(token, newPassword);
		return ResponseEntity.ok("Password reset successfully");
	}
}