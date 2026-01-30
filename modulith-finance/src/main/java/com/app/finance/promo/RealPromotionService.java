package com.app.finance.promo;

import com.app.finance.promo.entities.PromotionRule;
import com.app.finance.promo.repositories.PromotionRuleRepo;
import com.app.governance.rules.RuleEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Promotion Engine powered by SpEL Rules.
 */
@Service
@Primary
public class RealPromotionService implements PromotionService {

    private static final Logger log = LoggerFactory.getLogger(RealPromotionService.class);

    private final PromotionRuleRepo ruleRepo;
    private final RuleEngineService ruleEngine;

    public RealPromotionService(PromotionRuleRepo ruleRepo, RuleEngineService ruleEngine) {
        this.ruleRepo = ruleRepo;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public BigDecimal applyCoupon(String code, BigDecimal subTotal, String email) {
        log.info("Evaluating promotions for coupon: {} and subtotal: {}", code, subTotal);

        List<PromotionRule> rules = ruleRepo.findActiveRulesByCoupon(code);
        if (rules.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDiscount = BigDecimal.ZERO;

        // Context for SpEL evaluation
        Map<String, Object> context = new HashMap<>();
        context.put("subtotal", subTotal);
        context.put("email", email);
        context.put("code", code);

        for (PromotionRule rule : rules) {
            // 1. Evaluate Condition
            boolean applies = ruleEngine.evaluate(rule.getConditionExpression(), context);

            if (applies) {
                log.info("Promotion Rule '{}' matched.", rule.getName());

                // 2. Evaluate Action (e.g., "#subtotal * 0.10" or "50")
                BigDecimal discount = ruleEngine.evaluate(rule.getActionExpression(), context, BigDecimal.class);

                if (discount != null) {
                    totalDiscount = totalDiscount.add(discount);
                }

                if (rule.isExclusive()) {
                    break;
                }
            }
        }

        return totalDiscount.negate();
    }
}
