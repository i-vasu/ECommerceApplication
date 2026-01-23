package com.app.tests.fashion.pricing;

import com.app.order.entities.Cart;
import com.app.order.entities.CartItem;
import com.app.shipping.TaxCalculationService;
import com.app.shipping.TaxCalculationService.TaxCalculation;
import com.app.shipping.services.TaxCalculationServiceERPNextImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;

/**
 * Enterprise Pricing Accuracy Matrix.
 * Verifies complex tax scenarios (IGST vs CGST/SGST) across different states.
 * Ensures the 'Penny Discrepancy' is avoided by matching ERPNext logic.
 */
public class PricingAccuracyTest {

    private final TaxCalculationService taxService = new TaxCalculationServiceERPNextImpl(null); // No ERPNextService
                                                                                                 // needed for mock
                                                                                                 // logic

    @ParameterizedTest
    @CsvSource({
            "Maharashtra, 1000.0, 180.0, CGST, SGST",
            "Karnataka, 1000.0, 180.0, IGST, NULL",
            "Delhi, 550.5, 99.09, IGST, NULL",
            "Maharashtra, 550.5, 99.09, CGST, SGST"
    })
    @DisplayName("Verify Tax Accuracy across States")
    void testTaxCalculationAccuracy(String state, double cartTotal, double expectedTax, String tax1, String tax2) {
        Cart cart = new Cart();
        cart.setTotalPrice(cartTotal);

        TaxCalculation result = taxService.calculateGST(cart, state);

        assertEquals(expectedTax, result.totalAmount(), 0.01, "Total tax should match expected value for " + state);

        boolean foundTax1 = result.components().stream().anyMatch(c -> c.name().equalsIgnoreCase(tax1));
        assertTrue(foundTax1, "Should contain " + tax1);

        if (!"NULL".equals(tax2)) {
            boolean foundTax2 = result.components().stream().anyMatch(c -> c.name().equalsIgnoreCase(tax2));
            assertTrue(foundTax2, "Should contain " + tax2);
        }
    }
}
