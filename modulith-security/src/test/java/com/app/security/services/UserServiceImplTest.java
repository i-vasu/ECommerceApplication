package com.app.security.services;

import com.app.core.APIException;
import com.app.core.services.RedisLockService;
import com.app.governance.states.OperationalStateMachineService;
import com.app.security.entities.User;
import com.app.security.entities.UserLoyalty;
import com.app.security.entities.UserProfile;
import com.app.security.mappers.IdentityMapper;
import com.app.security.payloads.UserDTO;
import com.app.security.repositories.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Improved unit tests for UserServiceImpl.
 * Tests actual business logic with proper mocking.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepo userRepo;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private RedisLockService lockService;
    
    @Mock
    private IdentityMapper identityMapper;
    
    @Mock
    private OperationalStateMachineService stateMachineService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    private UserDTO validUserDTO;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validUserDTO = UserDTO.builder()
                .email("test@example.com")
                .password("Pass@123")
                .firstName("John")
                .lastName("Doe")
                .mobileNumber("1234567890")
                .build();

        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setPassword("encodedPassword");
        
        UserProfile profile = new UserProfile();
        profile.setFirstName("John");
        profile.setLastName("Doe");
        profile.setMobileNumber("1234567890");
        mockUser.setProfile(profile);
        
        UserLoyalty loyalty = new UserLoyalty();
        loyalty.setCustomerGroup("RETAIL");
        loyalty.setRewardPoints(0);
        mockUser.setLoyalty(loyalty);
    }

    @Test
    @DisplayName("Should successfully register user with valid data")
    void testRegisterUser_Success() {
        // Arrange
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(identityMapper.userDTOToUser(any(UserDTO.class))).thenReturn(mockUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepo.save(any(User.class))).thenReturn(mockUser);
        when(identityMapper.userToUserDTO(any(User.class))).thenReturn(validUserDTO);

        // Act
        UserDTO result = userService.registerUser(validUserDTO);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.email());
        verify(lockService).tryLock(eq("register:test@example.com"), any(Duration.class));
        verify(userRepo).save(any(User.class));
        verify(lockService).unlock(eq("register:test@example.com"));
        verify(stateMachineService).triggerAccountEvent(anyLong(), any());
    }

    @Test
    @DisplayName("Should reject weak password")
    void testRegisterUser_WeakPassword_ShouldThrowException() {
        // Arrange
        UserDTO weakPasswordDTO = validUserDTO.toBuilder()
                .password("weak")
                .build();
        
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);

        // Act & Assert
        APIException exception = assertThrows(APIException.class, () -> {
            userService.registerUser(weakPasswordDTO);
        });

        assertTrue(exception.getMessage().contains("Password must be 8+ chars"));
        verify(lockService).unlock(anyString());
    }

    @Test
    @DisplayName("Should reject password without uppercase")
    void testRegisterUser_NoUppercase_ShouldThrowException() {
        // Arrange
        UserDTO noUppercaseDTO = validUserDTO.toBuilder()
                .password("pass@123")
                .build();
        
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);

        // Act & Assert
        assertThrows(APIException.class, () -> {
            userService.registerUser(noUppercaseDTO);
        });
        
        verify(lockService).unlock(anyString());
    }

    @Test
    @DisplayName("Should reject password without special character")
    void testRegisterUser_NoSpecialChar_ShouldThrowException() {
        // Arrange
        UserDTO noSpecialCharDTO = validUserDTO.toBuilder()
                .password("Pass1234")
                .build();
        
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);

        // Act & Assert
        assertThrows(APIException.class, () -> {
            userService.registerUser(noSpecialCharDTO);
        });
        
        verify(lockService).unlock(anyString());
    }

    @Test
    @DisplayName("Should prevent concurrent registration for same email")
    void testRegisterUser_ConcurrentRegistration_ShouldThrowException() {
        // Arrange
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(false);

        // Act & Assert
        APIException exception = assertThrows(APIException.class, () -> {
            userService.registerUser(validUserDTO);
        });

        assertEquals("Registration is already in progress for this email.", exception.getMessage());
        verify(lockService, never()).unlock(anyString());
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should handle duplicate email gracefully")
    void testRegisterUser_DuplicateEmail_ShouldThrowException() {
        // Arrange
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(identityMapper.userDTOToUser(any(UserDTO.class))).thenReturn(mockUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepo.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for email"));

        // Act & Assert
        APIException exception = assertThrows(APIException.class, () -> {
            userService.registerUser(validUserDTO);
        });

        assertTrue(exception.getMessage().contains("User already exists with emailId"));
        verify(lockService).unlock(anyString());
    }

    @Test
    @DisplayName("Should handle duplicate mobile number gracefully")
    void testRegisterUser_DuplicateMobile_ShouldThrowException() {
        // Arrange
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(identityMapper.userDTOToUser(any(UserDTO.class))).thenReturn(mockUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepo.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for mobile_number"));

        // Act & Assert
        APIException exception = assertThrows(APIException.class, () -> {
            userService.registerUser(validUserDTO);
        });

        assertTrue(exception.getMessage().contains("User already exists with mobile number"));
        verify(lockService).unlock(anyString());
    }

    @Test
    @DisplayName("Should encode password if not already encoded")
    void testRegisterUser_PasswordEncoding() {
        // Arrange
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(identityMapper.userDTOToUser(any(UserDTO.class))).thenReturn(mockUser);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encodedPassword");
        when(userRepo.save(any(User.class))).thenReturn(mockUser);
        when(identityMapper.userToUserDTO(any(User.class))).thenReturn(validUserDTO);

        // Act
        userService.registerUser(validUserDTO);

        // Assert
        verify(passwordEncoder).encode(anyString());
        verify(lockService).unlock(anyString());
    }

    @Test
    @DisplayName("Should set verification code and expiry")
    void testRegisterUser_VerificationCodeSet() {
        // Arrange
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(identityMapper.userDTOToUser(any(UserDTO.class))).thenReturn(mockUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertNotNull(user.getVerificationCode(), "Verification code should be set");
            assertNotNull(user.getVerificationCodeExpiry(), "Verification expiry should be set");
            assertFalse(user.isVerified(), "User should not be verified initially");
            assertEquals("PENDING_VERIFICATION", user.getAccountStatus());
            return user;
        });
        when(identityMapper.userToUserDTO(any(User.class))).thenReturn(validUserDTO);

        // Act
        userService.registerUser(validUserDTO);

        // Assert
        verify(userRepo).save(any(User.class));
        verify(lockService).unlock(anyString());
    }
}
