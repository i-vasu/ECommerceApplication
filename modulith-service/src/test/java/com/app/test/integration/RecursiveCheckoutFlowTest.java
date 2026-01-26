package com.app.test.integration;

import com.app.order.entities.*;
import com.app.order.services.OptimizedCheckoutService;
import com.app.order.services.OptimizedCheckoutService.CheckoutResult;
import com.app.identity.entities.Address;
import com.app.commerce.promotion.repositories.PromotionRuleRepo;
import com.app.commerce.promotion.entities.PromotionRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Granular Recursive Flow Test.
 * Verifies the deep integration between Cart, Promotions, Tax, and Persistence.
 */
@SpringBootTest
@Transactional
public class RecursiveCheckoutFlowTest {

    @Autowired
    private OptimizedCheckoutService checkoutService;

    @Autowired
    private PromotionRuleRepo promotionRuleRepo;

    @Test
    @DisplayName("🔍 Recursive Check: Cart -> SpEL Promo -> ERPNext Tax -> Detailed Snapshot")
    void testDeepCheckoutFlow() {
        // 1. Setup Cart
        Cart cart = new Cart();
        com.app.identity.entities.User user = new com.app.identity.entities.User();
        user.setEmail("test@fashion.com");
        cart.setUser(user);
        cart.setCartId(1L);
        cart.setTotalPrice(10000.0);

        CartItem item = new CartItem();
        item.setProductId(101L);
        item.setItemCode("TSHIRT-RED-XL");
        item.setProductPrice(10000.0);
        item.setQuantity(1);
        cart.setCartItems(List.of(item));

        // 2. Setup a SpEL Promotion Rule (The 'Broadleaf-Killer' flexibility)
        PromotionRule rule = new PromotionRule();
        rule.setName("Festive 15% Support");
        rule.setConditionExpression("#subtotal > 5000");
        rule.setActionExpression("#subtotal * 0.15");
        rule.setActive(true);
        promotionRuleRepo.save(rule);

        // 3. Setup Shipping Address
        Address address = new Address();
        address.setState("Maharashtra");
        address.setPincode("400001");

        // 4. TRIGGER RECURSIVE FLOW
        CheckoutResult result = checkoutService.processCheckout(cart, address, null);

        // 5. VERIFY GRANULAR OUTPUTS
        assertNotNull(result);

        // A. Verify Promotion Engine recursion
        // Expecting 15% discount on 10000 = 1500
        assertEquals(-1500.0, result.promotionDiscount().doubleValue(), "Promotion Engine failed to recurse SpEL rule");

        // B. Verify Tax Engine recursion (IGST vs CGST/SGST)
        // Maharashtra is intra-state (simulated), expect CGST/SGST
        assertTrue(result.tax().components().size() >= 2, "Tax Engine failed to identify intra-state components");
        assertTrue(result.tax().components().stream().anyMatch(c -> c.name().contains("CGST")));

        // C. Verify Final Accounting Precision
        double expected = 10000.0 - 1500.0 + result.tax().totalAmount() + result.shipping().amount();
        assertEquals(expected, result.finalAmount(), 0.01, "Accounting discrepancy in recursive checkout");

        System.out.println("✅ Deep recursion test passed: Promotion, Tax, and Pricing are perfectly synced.");
    }
}
