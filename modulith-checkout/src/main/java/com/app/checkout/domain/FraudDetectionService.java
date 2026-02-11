package com.app.checkout.domain;

import com.app.core.contracts.CartContract;
import com.app.governance.rules.RuleEngineService;
import com.app.security.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Autonomous Fraud Detection Service.
 * Uses SpEL rules to calculate a risk score for transactions.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class FraudDetectionService {

    private final RuleEngineService ruleEngine;

    public int calculateRiskScore(CartContract cart, User user) {
        int score = 0;
        Map<String, Object> context = new HashMap<>();
        context.put("total", cart.subTotal());
        context.put("user", user);
        context.put("itemCount", cart.items().size());

        // Rule 1: High value order from new user
        if (ruleEngine.evaluate("total > 50000 && user.rewardPoints == 0", context)) {
            score += 50;
        }

        // Rule 2: Excessive items in a single cart
        if (ruleEngine.evaluate("itemCount > 20", context)) {
            score += 30;
        }

        // Rule 3: Guest-like behavior or specific email patterns
        if (ruleEngine.evaluate("user.email.endsWith('.xyz') || user.email.contains('temp')", context)) {
            score += 20;
        }

        log.info("Fraud Check for user {}: Risk Score {}", user.getEmail(), score);
        return score;
    }

    public boolean isFraudulent(CartContract cart, User user) {
        return calculateRiskScore(cart, user) >= 80;
    }
}
