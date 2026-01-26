package com.app.commerce.promotion.services;

import com.app.commerce.promotion.PromotionService;
import com.app.commerce.promotion.entities.PromotionRule;
import com.app.commerce.promotion.repositories.PromotionRuleRepo;
import com.app.commerce.pricing.contracts.OrderSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Advanced SpEL-based Promotion Engine.
 * Matches or exceeds Broadleaf's Offer Engine flexibility by allowing
 * dynamic rule evaluation without recompilation.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class RuleBasedPromotionEngine implements PromotionService {

    private final PromotionRuleRepo ruleRepo;
    private final com.app.identity.repositories.UserRepo userRepo;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public BigDecimal applyCoupon(String code, BigDecimal subTotal, String email) {
        List<PromotionRule> rules = ruleRepo.findActiveRulesByCoupon(code);

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("subtotal", subTotal);
        context.setVariable("email", email);

        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (PromotionRule rule : rules) {
            // SEGMENT VALIDATION (Broadleaf Parity)
            if (rule.getCustomerSegmentId() != null) {
                var user = userRepo.findByEmail(email).orElse(null);
                boolean inSegment = user != null && user.getSegments().stream()
                        .anyMatch(s -> s.getId().equals(rule.getCustomerSegmentId()));
                if (!inSegment) {
                    log.debug("User {} not in segment {} for rule {}", email, rule.getCustomerSegmentId(),
                            rule.getName());
                    continue;
                }
            }

            try {
                Boolean matches = parser.parseExpression(rule.getConditionExpression()).getValue(context,
                        Boolean.class);

                if (Boolean.TRUE.equals(matches)) {
                    log.info("Promotion Rule '{}' matched for coupon {}", rule.getName(), code);

                    // Evaluate action (e.g. "subtotal * 0.10")
                    BigDecimal discount = parser.parseExpression(rule.getActionExpression()).getValue(context,
                            BigDecimal.class);
                    totalDiscount = totalDiscount.add(discount);
                }
            } catch (Exception e) {
                log.error("Error evaluating promotion rule {}: {}", rule.getName(), e.getMessage());
            }
        }

        return totalDiscount.negate(); // Return as negative for order summary
    }
}
