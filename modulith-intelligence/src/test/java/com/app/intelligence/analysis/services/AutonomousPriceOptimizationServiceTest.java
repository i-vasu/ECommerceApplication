package com.app.intelligence.analysis.services;

import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Quality behavioral tests for AutonomousPriceOptimizationService.
 * Verifies dynamic pricing logic based on market demand and stock levels.
 */
@ExtendWith(MockitoExtension.class)
class AutonomousPriceOptimizationServiceTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private AutonomousPriceOptimizationService priceOptimizationService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("BEHAVIOR: Surge price check (Stock < 10 AND Views > 100)")
    void testSurgePricing_HighDemandLowStock() {
        // Arrange: Product with high demand and low stock
        Product product = createTestProduct(1L, "Hot Item", 1000.0, 900.0, 5);
        when(productRepo.findAll()).thenReturn(List.of(product));
        when(zSetOperations.score(anyString(), anyString())).thenReturn(150.0); // High demand

        // Act
        priceOptimizationService.runPriceOptimization();

        // Assert: Price should increase by 10% (900 * 1.1 = 990)
        verify(productRepo).save(argThat(p -> 
            p.getSpecialPrice().compareTo(BigDecimal.valueOf(990.00)) == 0
        ));
    }

    @Test
    @DisplayName("BEHAVIOR: Clearance check (Stock > 50 AND Views < 5)")
    void testClearancePricing_LowDemandHighStock() {
        // Arrange: Product with low demand and excessive stock
        Product product = createTestProduct(2L, "Slow Item", 1000.0, 800.0, 60);
        when(productRepo.findAll()).thenReturn(List.of(product));
        when(zSetOperations.score(anyString(), anyString())).thenReturn(3.0); // Low demand

        // Act
        priceOptimizationService.runPriceOptimization();

        // Assert: Price should decrease by 10% (800 * 0.9 = 720)
        verify(productRepo).save(argThat(p -> 
            p.getSpecialPrice().compareTo(BigDecimal.valueOf(720.00)) == 0
        ));
    }

    @Test
    @DisplayName("EDGE CASE: Do not surge if already capped (Surge <= Original * 1.2)")
    void testSurgePricing_CappedAt120Percent() {
        // Arrange: Already at 1150, another 10% would be 1265 (Above 1200 cap)
        Product product = createTestProduct(3L, "Premium Item", 1000.0, 1150.0, 2);
        when(productRepo.findAll()).thenReturn(List.of(product));
        when(zSetOperations.score(anyString(), anyString())).thenReturn(500.0);

        // Act
        priceOptimizationService.runPriceOptimization();

        // Assert: Price must not follow surge increase as it exceeds cap (1000 * 1.2 = 1200)
        // Service logic check: if (surgePrice.compareTo(maxPrice) < 0)
        verify(productRepo).save(argThat(p -> 
            p.getSpecialPrice().compareTo(BigDecimal.valueOf(1150.00)) == 0
        ));
    }

    @Test
    @DisplayName("EDGE CASE: Do not drop below 50% of original price")
    void testClearancePricing_FloorProtection() {
        // Arrange: Current 550, original 1000. Another 10% drop = 495 (Below 500 floor)
        Product product = createTestProduct(4L, "Bottom Item", 1000.0, 550.0, 100);
        when(productRepo.findAll()).thenReturn(List.of(product));
        when(zSetOperations.score(anyString(), anyString())).thenReturn(1.0);

        // Act
        priceOptimizationService.runPriceOptimization();

        // Assert: Floor prevents drop (1000 * 0.5 = 500)
        verify(productRepo).save(argThat(p -> 
            p.getSpecialPrice().compareTo(BigDecimal.valueOf(550.00)) == 0
        ));
    }

    @Test
    @DisplayName("BEHAVIOR: Normal conditions (Stock=20, Views=50) -> No change")
    void testNormalConditions_NoPriceChange() {
        // Arrange
        Product product = createTestProduct(5L, "Regular Item", 1000.0, 1000.0, 20);
        when(productRepo.findAll()).thenReturn(List.of(product));
        when(zSetOperations.score(anyString(), anyString())).thenReturn(50.0);

        // Act
        priceOptimizationService.runPriceOptimization();

        // Assert
        verify(productRepo).save(argThat(p -> 
            p.getSpecialPrice().compareTo(BigDecimal.valueOf(1000.00)) == 0
        ));
    }

    private Product createTestProduct(Long id, String name, double price, double special, int qty) {
        Product p = new Product();
        p.setProductId(id);
        p.setProductName(name);
        p.setPrice(BigDecimal.valueOf(price));
        p.setSpecialPrice(BigDecimal.valueOf(special));
        p.setQuantity(qty);
        return p;
    }
}
