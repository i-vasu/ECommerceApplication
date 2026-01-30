package com.app.finance.promo.services;

import com.app.finance.promo.PromotionService;
import com.app.finance.promo.entities.PromotionRule;
import com.app.finance.promo.repositories.PromotionRuleRepo;
import com.app.finance.pricing.contracts.OrderSummary;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced SpEL-based Promotion Engine.
 * Matches or exceeds Broadleaf's Offer Engine flexibility by allowing
 * dynamic rule evaluation without recompilation.
 */
@Service
public class RuleBasedPromotionEngine implements PromotionService {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedPromotionEngine.class);

    private final PromotionRuleRepo ruleRepo;
    private final com.app.security.repositories.UserRepo userRepo;
    private final com.app.governance.rules.RuleEngineService ruleEngine;

    public RuleBasedPromotionEngine(PromotionRuleRepo ruleRepo, com.app.security.repositories.UserRepo userRepo,
            com.app.governance.rules.RuleEngineService ruleEngine) {
        this.ruleRepo = ruleRepo;
        this.userRepo = userRepo;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public BigDecimal applyCoupon(String code, BigDecimal subTotal, String email) {
        List<PromotionRule> rules = ruleRepo.findActiveRulesByCoupon(code);

        Map<String, Object> context = new java.util.HashMap<>();
        context.put("subtotal", subTotal);
        context.put("email", email);

        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (PromotionRule rule : rules) {
            // SEGMENT VALIDATION
            if (rule.getCustomerSegmentId() != null) {
                var user = userRepo.findByEmail(email).orElse(null);
                boolean inSegment = user != null && user.getSegments().stream()
                        .anyMatch(s -> s.getId().equals(rule.getCustomerSegmentId()));
                if (!inSegment)
                    continue;
            }

            // DYNAMIC EVALUATION via Unified Engine
            if (ruleEngine.evaluate(rule.getConditionExpression(), context)) {
                log.info("Promotion Rule '{}' matched for coupon {}", rule.getName(), code);
                BigDecimal discount = ruleEngine.evaluate(rule.getActionExpression(), context, BigDecimal.class);
                if (discount != null) {
                    totalDiscount = totalDiscount.add(discount);
                }
            }
        }
        return totalDiscount.negate();
    }
}
