package com.app.marketing.services;

import com.app.marketing.entities.UserTrait;
import com.app.marketing.repositories.UserTraitRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Quality behavioral tests for TraitCalculationService.
 * Focuses on the "Nightly Job" pattern: Data extraction from SQL and persistence to UserProfile.
 */
@ExtendWith(MockitoExtension.class)
class TraitCalculationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserTraitRepo userTraitRepo;

    @InjectMocks
    private TraitCalculationService traitCalculationService;

    @Test
    @DisplayName("BEHAVIOR: Should calculate traits and identify VIP user (total_spent > 10000)")
    void testCalculateAllTraits_VIPUser() {
        // Arrange
        Map<String, Object> traitsMap = new HashMap<>();
        traitsMap.put("total_spent", 15000.0);
        traitsMap.put("is_vip", true);

        Map<String, Object> stats = new HashMap<>();
        stats.put("email", "vip@example.com");
        stats.put("traits", traitsMap);

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(stats));
        when(userTraitRepo.findById("vip@example.com")).thenReturn(Optional.empty());

        // Act
        traitCalculationService.calculateAllTraits();

        // Assert
        verify(userTraitRepo).save(argThat(ut -> 
            ut.getEmail().equals("vip@example.com") && 
            (boolean) ut.getTraits().get("is_vip") == true
        ));
    }

    @Test
    @DisplayName("BEHAVIOR: Should update existing traits without duplication")
    @SuppressWarnings("unchecked")
    void calculateAllTraits_UpdateExisting() {
        // Arrange
        Map<String, Object> newTraits = Map.of("total_spent", 500.0, "is_vip", false);
        Map<String, Object> stats = Map.of("email", "user@example.com", "traits", newTraits);

        UserTrait existing = new UserTrait("user@example.com", new HashMap<>(Map.of("is_vip", true)));

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(stats));
        when(userTraitRepo.findById("user@example.com")).thenReturn(Optional.of(existing));

        // Act
        traitCalculationService.calculateAllTraits();

        // Assert
        verify(userTraitRepo).save(argThat(ut -> 
            ut.getTraits().get("total_spent").equals(500.0) &&
            (boolean) ut.getTraits().get("is_vip") == false
        ));
    }
}
