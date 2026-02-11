package com.app.logistics.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Quality behavioral tests for InventoryReservationService.
 * Focuses on atomic stock management and reservation tracking via Redis Lua scripts.
 */
@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private InventoryReservationService reservationService;

    @BeforeEach
    void setUp() {
        // We can't easily mock redisTemplate.execute for Lua scripts without deep mocking
        // But we can verify the other operations
    }

    @Test
    @DisplayName("BEHAVIOR: Should return true when atomic reservation succeeds")
    void reserveStock_Success() {
        // Arrange
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(50L); // 50 left
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // Act
        boolean result = reservationService.reserveStock("ITEM001", 5);

        // Assert
        assertTrue(result);
        verify(zSetOperations).add(eq("inventory:reservations:expiry"), anyString(), anyDouble());
    }

    @Test
    @DisplayName("EDGE CASE: Should return false when stock is insufficient (Result -2)")
    void reserveStock_InsufficientStock() {
        // Arrange
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(-2L);

        // Act
        boolean result = reservationService.reserveStock("ITEM001", 100);

        // Assert
        assertFalse(result);
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("BEHAVIOR: Should release stock back to inventory (Increment operation)")
    void releaseStock_ShouldIncrementRedisValue() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        reservationService.releaseStock("ITEM001", 10);

        // Assert
        verify(valueOperations).increment("inventory:stock:ITEM001", 10);
    }

    @Test
    @DisplayName("BEHAVIOR: Should confirm stock by removing from expiry set")
    void confirmStock_ShouldRemoveEntry() {
        // Arrange
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.remove(eq("inventory:reservations:expiry"), anyString())).thenReturn(1L);

        // Act
        reservationService.confirmStock("ITEM001", 5, "lock-123");

        // Assert
        verify(zSetOperations).remove(eq("inventory:reservations:expiry"), eq("ITEM001:lock-123:5"));
    }

    @Test
    @DisplayName("BEHAVIOR: Check stock should read from Redis")
    void checkStock_ShouldReturnTrueIfAvailable() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("inventory:stock:ITEM001")).thenReturn("20");

        // Act
        boolean result = reservationService.checkStock("ITEM001", 15);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("EDGE CASE: Check stock should trigger Read-Through on Redis Miss")
    void checkStock_OnMiss_ShouldInitializeAndReturnResult() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("inventory:stock:NEW-ITEM")).thenReturn(null);

        // Act
        boolean result = reservationService.checkStock("NEW-ITEM", 50);

        // Assert
        assertTrue(result); // Fallback is 100
        verify(valueOperations).set(eq("inventory:stock:NEW-ITEM"), eq("100"), any());
    }
}
