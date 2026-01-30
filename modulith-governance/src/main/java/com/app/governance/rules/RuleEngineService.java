package com.app.governance.rules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic Rules Engine powered by Spring Expression Language (SpEL).
 * Allows evaluating business logic defined as strings in the database or
 * config.
 */
@Service
public class RuleEngineService {
    private static final Logger log = LogManager.getLogger(RuleEngineService.class);
    private final com.app.governance.audit.OperationalAuditRepo auditRepo;
    private final SystemRuleRepo ruleRepo;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    private final Map<String, String> keyToExpressionCache = new ConcurrentHashMap<>();

    public RuleEngineService(com.app.governance.audit.OperationalAuditRepo auditRepo, SystemRuleRepo ruleRepo) {
        this.auditRepo = auditRepo;
        this.ruleRepo = ruleRepo;
    }

    /**
     * Evaluates a rule fetched from the database by its key.
     */
    public boolean evaluateByKey(String ruleKey, Map<String, Object> contextVariables, boolean defaultValue) {
        String expressionStr = keyToExpressionCache.computeIfAbsent(ruleKey,
                key -> ruleRepo.findByRuleKeyAndActiveTrue(key)
                        .map(SystemRule::getExpression)
                        .orElse(null));

        if (expressionStr == null) {
            log.warn("Rule key '{}' not found in database. Using default value: {}", ruleKey, defaultValue);
            return defaultValue;
        }

        return evaluate(expressionStr, contextVariables);
    }

    /**
     * Evaluates a rule and returns a typed result from DB lookup.
     */
    public <T> T evaluateByKey(String ruleKey, Map<String, Object> contextVariables, Class<T> desiredType,
            T defaultValue) {
        String expressionStr = keyToExpressionCache.computeIfAbsent(ruleKey,
                key -> ruleRepo.findByRuleKeyAndActiveTrue(key)
                        .map(SystemRule::getExpression)
                        .orElse(null));

        if (expressionStr == null)
            return defaultValue;

        return evaluate(expressionStr, contextVariables, desiredType);
    }

    /**
     * Evaluates a rule against a given root object.
     * 
     * @param rule             The SpEL rule string (e.g., "#order.total > 1000")
     * @param contextVariables Variables to inject into the evaluation context.
     * @return Boolean result of the rule.
     */
    public boolean evaluate(String rule, Map<String, Object> contextVariables) {
        try {
            Expression exp = expressionCache.computeIfAbsent(rule, parser::parseExpression);
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariables(contextVariables);

            Boolean result = exp.getValue(context, Boolean.class);
            boolean finalResult = result != null && result;

            // Async log for dashboard
            logAudit("RULE_EVALUATION", rule, String.valueOf(finalResult), true);

            return finalResult;
        } catch (Exception e) {
            log.error("Rule evaluation failed: {} (Rule: {})", e.getMessage(), rule);
            logAudit("RULE_EVALUATION", rule, "ERROR: " + e.getMessage(), false);
            return false;
        }
    }

    /**
     * Evaluates a rule and returns a typed result.
     */
    public <T> T evaluate(String rule, Map<String, Object> contextVariables, Class<T> desiredType) {
        try {
            Expression exp = expressionCache.computeIfAbsent(rule, parser::parseExpression);
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariables(contextVariables);

            return exp.getValue(context, desiredType);
        } catch (Exception e) {
            log.error("Rule evaluation failed: {} (Rule: {})", e.getMessage(), rule);
            return null;
        }
    }

    public void clearCache() {
        expressionCache.clear();
        keyToExpressionCache.clear();
        log.info("Rule Engine cache cleared.");
    }

    private void logAudit(String type, String detail, String result, boolean success) {
        try {
            auditRepo.save(com.app.governance.audit.OperationalAudit.builder()
                    .type(type)
                    .category("Engine")
                    .detail(detail.length() > 250 ? detail.substring(0, 250) + "..." : detail)
                    .result(result)
                    .success(success)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to save operational audit: {}", e.getMessage());
        }
    }
}
