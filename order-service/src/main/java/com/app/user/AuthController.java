package com.app.user;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.exceptions.UserNotFoundException;
import com.app.payloads.LoginCredentials;
import com.app.payloads.UserDTO;
import com.app.security.JWTUtil;
import com.app.entites.RefreshToken;
import com.app.entites.User;
import com.app.payloads.TokenRefreshRequest;
import com.app.payloads.TokenRefreshResponse;
import com.app.services.RefreshTokenService;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.repositories.UserRepo;
import com.app.services.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-Commerce Application")
public class AuthController {

	@Autowired
	private UserService userService;

	@Autowired
	private JWTUtil jwtUtil;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private UserRepo userRepo;

	@PostMapping("/register")
	public ResponseEntity<Map<String, Object>> registerHandler(@Valid @RequestBody UserDTO user)
			throws UserNotFoundException {
		String encodedPass = passwordEncoder.encode(user.getPassword());

		user.setPassword(encodedPass);

		UserDTO userDTO = userService.registerUser(user);

		String token = jwtUtil.generateToken(userDTO.getEmail());

		return new ResponseEntity<Map<String, Object>>(Collections.singletonMap("jwt-token", token),
				HttpStatus.CREATED);
	}

	@PostMapping("/login")
	public Map<String, Object> loginHandler(@Valid @RequestBody LoginCredentials credentials) {

		UsernamePasswordAuthenticationToken authCredentials = new UsernamePasswordAuthenticationToken(
				credentials.getEmail(), credentials.getPassword());

		authenticationManager.authenticate(authCredentials);

		String token = jwtUtil.generateToken(credentials.getEmail());

		User user = userRepo.findByEmail(credentials.getEmail())
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", credentials.getEmail()));

		if (!user.isVerified()) {
			throw new APIException("Email is not verified. Please verify your email.");
		}

		RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserId());

		Map<String, Object> response = new java.util.HashMap<>();
		response.put("jwt-token", token);
		response.put("refresh-token", refreshToken.getToken());
		return response;
	}

	@PostMapping("/refresh-token")
	public ResponseEntity<?> refreshtoken(@Valid @RequestBody TokenRefreshRequest request) {
		String requestRefreshToken = request.getRefreshToken();

		return refreshTokenService.findByToken(requestRefreshToken)
				.map(refreshTokenService::verifyExpiration)
				.map(RefreshToken::getUser)
				.map(user -> {
					String token = jwtUtil.generateToken(user.getEmail());
					return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
				})
				.orElseThrow(() -> new APIException("Refresh token is not in database!"));
	}

	@PostMapping("/verify-email")
	public ResponseEntity<String> verifyEmail(@RequestParam String email, @RequestParam String code) {
		userService.verifyEmail(email, code);
		return ResponseEntity.ok("Email verified successfully");
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<String> forgotPassword(@RequestParam String email) {
		userService.forgotPassword(email);
		return ResponseEntity.ok("Password reset token sent to email");
	}

	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
		userService.resetPassword(token, newPassword);
		return ResponseEntity.ok("Password reset successfully");
	}
}