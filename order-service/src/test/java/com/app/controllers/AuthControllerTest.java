package com.app.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.app.entites.RefreshToken;
import com.app.entites.User;
import com.app.payloads.LoginCredentials;
import com.app.repositories.UserRepo;
import com.app.security.JWTUtil;
import com.app.services.RefreshTokenService;
import com.app.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JWTUtil jwtUtil;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private UserRepo userRepo;

    @MockBean
    private com.app.services.ERPNextService erpNextService;

    @MockBean
    private com.app.services.MinioService minioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testLogin() throws Exception {
        LoginCredentials credentials = new LoginCredentials();
        credentials.setEmail("test@example.com");
        credentials.setPassword("password");

        User user = new User();
        user.setUserId(1L);
        user.setEmail("test@example.com");
        user.setVerified(true);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);
        when(jwtUtil.generateToken(anyString())).thenReturn("jwt-token");

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk());
    }

    @Test
    public void testForgotPassword() throws Exception {
        doNothing().when(userService).forgotPassword(anyString());

        mockMvc.perform(post("/api/forgot-password")
                .param("email", "test@example.com"))
                .andExpect(status().isOk());
    }

}
