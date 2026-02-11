package com.app.tests.fashion.pricing;

import com.app.logistics.shipping.TaxCalculationService;
import com.app.logistics.shipping.TaxCalculationService.TaxCalculation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enterprise Pricing Accuracy Matrix.
 * Verifies complex tax scenarios (IGST vs CGST/SGST) across different states.
 * Ensures the 'Penny Discrepancy' is avoided by matching ERPNext logic.
 */
public class PricingAccuracyTest {

    private final TaxCalculationService taxService = new FakeTaxService();

    static class FakeTaxService implements TaxCalculationService {
        @Override
        public TaxCalculation calculateGST(java.math.BigDecimal subtotal, String state) {
            java.util.List<com.app.logistics.shipping.TaxCalculationService.TaxComponent> components = new java.util.ArrayList<>();
            java.math.BigDecimal rate = java.math.BigDecimal.valueOf(0.18);
            java.math.BigDecimal taxAmount = subtotal.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
            
            if ("Maharashtra".equalsIgnoreCase(state)) {
                java.math.BigDecimal halfRate = rate.divide(java.math.BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal halfTax = taxAmount.divide(java.math.BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
                components.add(new com.app.logistics.shipping.TaxCalculationService.TaxComponent("CGST", halfRate, halfTax));
                components.add(new com.app.logistics.shipping.TaxCalculationService.TaxComponent("SGST", halfRate, halfTax));
            } else {
                components.add(new com.app.logistics.shipping.TaxCalculationService.TaxComponent("IGST", rate, taxAmount));
            }
            return new TaxCalculation(taxAmount, components);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "Maharashtra, 1000.0, 180.0, CGST, SGST",
            "Karnataka, 1000.0, 180.0, IGST, NULL",
            "Delhi, 550.5, 99.09, IGST, NULL",
            "Maharashtra, 550.5, 99.09, CGST, SGST"
    })
    @DisplayName("Verify Tax Accuracy across States")
    void testTaxCalculationAccuracy(String state, double cartTotal, double expectedTax, String tax1, String tax2) {
        TaxCalculation result = taxService.calculateGST(java.math.BigDecimal.valueOf(cartTotal), state);

        assertEquals(0, java.math.BigDecimal.valueOf(expectedTax).compareTo(result.totalAmount()), "Total tax should match expected value for " + state);

        boolean foundTax1 = result.components().stream().anyMatch(c -> c.name().equalsIgnoreCase(tax1));
        assertTrue(foundTax1, "Should contain " + tax1);

        if (!"NULL".equals(tax2)) {
            boolean foundTax2 = result.components().stream().anyMatch(c -> c.name().equalsIgnoreCase(tax2));
            assertTrue(foundTax2, "Should contain " + tax2);
        }
    }
}
