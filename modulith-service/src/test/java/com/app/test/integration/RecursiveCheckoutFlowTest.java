package com.app.test.integration;

import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.checkout.pipeline.OptimizedCheckoutService;
import com.app.checkout.pipeline.OptimizedCheckoutService.CheckoutResult;
import com.app.finance.promo.entities.PromotionRule;
import com.app.finance.promo.repositories.PromotionRuleRepo;
import com.app.security.entities.Address;
import com.app.security.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @DisplayName("🔍 Recursive Check: Cart -> SpEL Promo -> Tax Engine -> Detailed Snapshot")
    void testDeepCheckoutFlow() {
        // 1. Setup Cart
        Cart cart = new Cart();
        User user = new User();
        user.setUserId(1L);
        user.setEmail("test@fashion.com");
        cart.setUserId(user.getUserId());
        cart.setCartId(1L);
        cart.setTotalPrice(BigDecimal.valueOf(10000.0));

        CartItem item = new CartItem();
        item.setProductId(101L);
        item.setItemCode("TSHIRT-RED-XL");
        item.setProductPrice(BigDecimal.valueOf(10000.0));
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
        com.app.core.contracts.CartContract cartContract = new com.app.core.contracts.CartContract(
                cart.getCartId(),
                cart.getUserId(),
                cart.getTotalPrice(),
                cart.getCouponCode(),
                cart.getCartItems().stream().map(i -> new com.app.core.contracts.CartContract.CartItemContract(
                        i.getProductId(),
                        i.getItemCode(),
                        i.getProductName(),
                        i.getQuantity(),
                        i.getProductPrice(),
                        i.getDiscount())).toList());
        CheckoutResult result = checkoutService.processCheckout(cartContract, address);

        // 5. VERIFY GRANULAR OUTPUTS
        assertNotNull(result);

        // A. Verify Promotion Engine recursion
        // Expecting 15% discount on 10000 = 1500
        assertThat(result.promotionDiscount().abs()).isEqualByComparingTo(BigDecimal.valueOf(1500.0));

        // B. Verify Tax Engine recursion (IGST vs CGST/SGST)
        // Maharashtra is intra-state (simulated), expect CGST/SGST
        assertTrue(result.tax().components().size() >= 2, "Tax Engine failed to identify intra-state components");
        assertTrue(result.tax().components().stream().anyMatch(c -> c.name().contains("CGST")));

        // C. Verify Final Accounting Precision
        BigDecimal expected = BigDecimal.valueOf(10000.0)
            .subtract(BigDecimal.valueOf(1500.0))
            .add(result.tax().totalAmount())
            .add(result.shipping().amount());
        assertThat(result.finalAmount()).isEqualByComparingTo(expected);

        System.out.println("✅ Deep recursion test passed: Promotion, Tax, and Pricing are perfectly synced.");
    }
}
