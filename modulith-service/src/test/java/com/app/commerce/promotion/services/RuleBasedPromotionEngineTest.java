package com.app.commerce.promotion.services;

import com.app.commerce.promotion.entities.PromotionRule;
import com.app.commerce.promotion.repositories.PromotionRuleRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RuleBasedPromotionEngineTest {

    @Mock
    private PromotionRuleRepo ruleRepo;

    @InjectMocks
    private RuleBasedPromotionEngine promotionEngine;

    private final String TEST_CODE = "SALE2026";
    private final String TEST_EMAIL = "vip@example.com";

    @Test
    @DisplayName("✅ Apply 10% discount when subtotal is above 5000")
    void testSubtotalRule() {
        PromotionRule rule = new PromotionRule();
        rule.setName("10% OFF 5000+");
        rule.setConditionExpression("#subtotal >= 5000");
        rule.setActionExpression("#subtotal * 0.10");

        when(ruleRepo.findActiveRulesByCoupon(TEST_CODE)).thenReturn(List.of(rule));

        BigDecimal result = promotionEngine.applyCoupon(TEST_CODE, new BigDecimal("6000"), TEST_EMAIL);

        // Expected result is -600.0 (as a negative value for pricing summary)
        assertEquals(new BigDecimal("-600.00"), result.setScale(2));
    }

    @Test
    @DisplayName("✅ Rule should not match if subtotal is below threshold")
    void testUnmatchedRule() {
        PromotionRule rule = new PromotionRule();
        rule.setConditionExpression("#subtotal >= 5000");
        rule.setActionExpression("#subtotal * 0.10");

        when(ruleRepo.findActiveRulesByCoupon(TEST_CODE)).thenReturn(List.of(rule));

        BigDecimal result = promotionEngine.applyCoupon(TEST_CODE, new BigDecimal("4000"), TEST_EMAIL);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("✅ Targeted promotion for specific email domains")
    void testEmailTargetingRule() {
        PromotionRule rule = new PromotionRule();
        rule.setName("Internal Staff Discount");
        rule.setConditionExpression("#email.endsWith('@example.com')");
        rule.setActionExpression("500.0"); // Flat 500 off

        when(ruleRepo.findActiveRulesByCoupon(TEST_CODE)).thenReturn(List.of(rule));

        BigDecimal result = promotionEngine.applyCoupon(TEST_CODE, new BigDecimal("1000"), TEST_EMAIL);

        assertEquals(new BigDecimal("-500.0"), result);
    }
}
