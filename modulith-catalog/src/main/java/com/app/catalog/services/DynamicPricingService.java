package com.app.catalog.services;

import com.app.governance.rules.RuleEngineService;
import com.app.catalog.entities.DynamicPricingRule;
import com.app.catalog.entities.Product;
import com.app.catalog.repositories.DynamicPricingRuleRepo;
import lombok.RequiredArgsConstructor;
// import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Dynamic Pricing Engine.
 * Evaluates SpEL-based rules to determine the effective price of a product for
 * a specific context.
 */
@Service
// @Log4j2
@RequiredArgsConstructor
public class DynamicPricingService {
    private static final Logger log = LoggerFactory.getLogger(DynamicPricingService.class);

    private final DynamicPricingRuleRepo ruleRepo;
    private final RuleEngineService ruleEngine;

    /**
     * Calculates the effective price for a product given a context (user, quantity,
     * etc.).
     */
    public BigDecimal calculatePrice(Product product, Map<String, Object> context) {
        BigDecimal currentPrice = product.getSpecialPrice() != null ? product.getSpecialPrice() : product.getPrice();

        List<DynamicPricingRule> rules = ruleRepo.findActiveRules();
        if (rules.isEmpty()) {
            return currentPrice;
        }

        Map<String, Object> evalContext = new HashMap<>(context);
        evalContext.put("product", product);
        evalContext.put("basePrice", currentPrice);

        for (DynamicPricingRule rule : rules) {
            try {
                boolean matches = ruleEngine.evaluate(rule.getConditionExpression(), evalContext);
                if (matches) {
                    log.debug("Pricing Rule '{}' matched for product {}", rule.getName(), product.getProductId());
                    BigDecimal adjustedPrice = ruleEngine.evaluate(rule.getPriceAdjustmentExpression(), evalContext,
                            BigDecimal.class);
                    if (adjustedPrice != null) {
                        currentPrice = adjustedPrice;
                    }
                }
            } catch (Exception e) {
                log.error("Error evaluating pricing rule {}: {}", rule.getName(), e.getMessage());
            }
        }

        return currentPrice;
    }
}
