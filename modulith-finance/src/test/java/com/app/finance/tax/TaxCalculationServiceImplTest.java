package com.app.finance.tax;

import com.app.catalog.ProductService;
import com.app.catalog.payloads.ProductDTO;
import com.app.finance.entities.TaxRate;
import com.app.finance.pricing.contracts.OrderTotalInput;
import com.app.finance.repositories.TaxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class TaxCalculationServiceImplTest {

    @Mock
    private ProductService productService;

    @Mock
    private TaxRateRepository taxRateRepository;

    private TaxCalculationServiceImpl taxService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        taxService = new TaxCalculationServiceImpl(productService, taxRateRepository);
    }

    @Test
    void calculateTax_shouldUseProductTaxRate_whenAvailable() {
        // Arrange
        Long productId = 101L;
        Long taxRateId = 5L;
        Double taxPercentage = 12.0;

        ProductDTO mockProduct = new ProductDTO(productId, "Test Product", "CODE123", "image.jpg", "Desc", 10, 
                BigDecimal.valueOf(1000.0), BigDecimal.ZERO, BigDecimal.ZERO, 
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 4.5, 
                null, "", null, null, null, null, taxRateId);

        TaxRate mockTaxRate = new TaxRate(taxRateId, "GST_12", "GST 12%", taxPercentage);

        when(productService.getProductById(productId)).thenReturn(mockProduct);
        when(taxRateRepository.findById(taxRateId)).thenReturn(Optional.of(mockTaxRate));

        OrderTotalInput.ItemInput item = new OrderTotalInput.ItemInput(productId, "CODE123", BigDecimal.valueOf(1000.0), 1);
        List<OrderTotalInput.ItemInput> items = List.of(item);

        // Act
        TaxCalculationService.TaxCalculation result = taxService.calculateTax(items, "Maharashtra");

        // Assert
        // 12% of 1000 = 120
        assertEquals(0, BigDecimal.valueOf(120.00).compareTo(result.totalAmount()));
        assertEquals(2, result.components().size()); // CGST + SGST
    }

    @Test
    void calculateTax_shouldFallbackTo18Percent_whenTaxRateIdMissing() {
        // Arrange
        Long productId = 102L;
        // No taxRateId

        ProductDTO mockProduct = new ProductDTO(productId, "Test Product 2", "CODE124", "image.jpg", "Desc", 10, 
                BigDecimal.valueOf(1000.0), BigDecimal.ZERO, BigDecimal.ZERO, 
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 4.5,
                null, "", null, null, null, null, null);

        when(productService.getProductById(productId)).thenReturn(mockProduct);

        OrderTotalInput.ItemInput item = new OrderTotalInput.ItemInput(productId, "CODE124", BigDecimal.valueOf(1000.0), 1);
        List<OrderTotalInput.ItemInput> items = List.of(item);

        // Act
        TaxCalculationService.TaxCalculation result = taxService.calculateTax(items, "Maharashtra");

        // Assert
        // Default 18% of 1000 = 180
        assertEquals(0, BigDecimal.valueOf(180.00).compareTo(result.totalAmount()));
    }
}
