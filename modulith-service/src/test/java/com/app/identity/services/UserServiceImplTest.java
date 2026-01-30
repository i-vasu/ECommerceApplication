package com.app.security.services;

import com.app.security.entities.User;
import com.app.security.payloads.UserDTO;
import com.app.security.repositories.UserRepo;
import com.app.security.repositories.RoleRepo;
import com.app.core.ResourceNotFoundException;
import com.app.core.APIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private RoleRepo roleRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("user@example.com");
        testUser.setPassword("encodedPassword");

        testUserDTO = new UserDTO(1L, "First", "Last", "1234567890", "user@example.com", "Password123", null, null);
    }

    @Test
    void testRegisterUser_Success() {
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(modelMapper.map(any(UserDTO.class), eq(User.class))).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserDTO.class))).thenReturn(testUserDTO);

        UserDTO registered = userService.registerUser(testUserDTO);

        assertNotNull(registered);
        assertEquals("user@example.com", registered.email());
        verify(userRepo).save(any());
    }

    @Test
    void testRegisterUser_AlreadyExists() {
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        
        assertThrows(APIException.class, () -> userService.registerUser(testUserDTO));
    }

    @Test
    void testGetUserByEmail_Found() {
        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        UserDTO found = userService.getUserByEmail("user@example.com");

        assertNotNull(found);
        assertEquals("user@example.com", found.email());
    }

    @Test
    void testGetUserByEmail_NotFound() {
        when(userRepo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("unknown@example.com"));
    }

    @Test
    void testDeactivateAccount() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        
        userService.deactivateAccount(1L);
        
        assertFalse(testUser.isEnabled());
        verify(userRepo).save(testUser);
    }
}
