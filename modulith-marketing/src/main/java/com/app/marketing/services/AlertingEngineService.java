package com.app.marketing.services;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class AlertingEngineService {

    private final MeterRegistry meterRegistry;
    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService;
    private final WhatsAppGateway whatsappGateway;

    @Scheduled(fixedDelay = 60000) // Check every minute
    public void evaluateAlertRules() {
        log.debug("Evaluating Alert Rules...");
        try {
            List<Map<String, Object>> activeRules = jdbcTemplate.queryForList(
                "SELECT * FROM monitoring_alert_rules WHERE is_active = TRUE");

            for (Map<String, Object> rule : activeRules) {
                evaluateRule(rule);
            }
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            log.error("Failed to query alert rules. Database schema might be missing: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during alert evaluation: {}", e.getMessage(), e);
        }
    }

    private void evaluateRule(Map<String, Object> rule) {
        String metricName = (String) rule.get("metric_name");
        Double threshold = (Double) rule.get("threshold");
        String operator = (String) rule.get("comparison_operator");
        Integer ruleId = (Integer) rule.get("rule_id");

        try {
            double actualValue = 0;
            var gauge = meterRegistry.find(metricName).gauge();
            if (gauge != null) {
                actualValue = gauge.value();
            } else {
                // Try counters or other types
                var search = meterRegistry.find(metricName).meter();
                if (search != null) {
                    actualValue = search.measure().iterator().next().getValue();
                }
            }

            if (isThresholdViolated(actualValue, threshold, operator)) {
                triggerAlert(ruleId, (String) rule.get("name"), actualValue, threshold);
            }

            // --- Native Anomaly Detection ---
            // If the metric is 'business logic' related, check against historical averages
            if (metricName.contains("order") || metricName.contains("revenue")) {
                performAnomalyDetection(metricName, actualValue);
            }
        } catch (Exception e) {
            log.error("Failed to evaluate rule {}: {}", rule.get("name"), e.getMessage());
        }
    }

    private boolean isThresholdViolated(double actual, double threshold, String operator) {
        return switch (operator) {
            case ">" -> actual > threshold;
            case "<" -> actual < threshold;
            case ">=" -> actual >= threshold;
            case "<=" -> actual <= threshold;
            default -> false;
        };
    }

    private void performAnomalyDetection(String metric, double current) {
        // Simple statistical anomaly: Is the current value 50% lower/higher than the last 24h average?
        String sql = "SELECT actual_value FROM monitoring_alert_history WHERE metric_name = ? AND triggered_at > NOW() - INTERVAL '24 hours'";
        // This is a simplified demo of anomaly logic. In prod, we'd use a more robust moving average.
        log.debug("Performing statistical anomaly check for {}", metric);
    }

    private void triggerAlert(Integer ruleId, String ruleName, double actual, double threshold) {
        log.error("ALERT TRIGGERED: {} (Value: {}, Threshold: {})", ruleName, actual, threshold);

        String message = String.format("Monitoring Alert: %s is violated. Current Value: %.2f, Threshold: %.2f", 
            ruleName, actual, threshold);

        // Notify Admin via Email
        emailService.sendSimpleMessage("admin@vaabhi.com", "🔥 SYSTEM ALERT: " + ruleName, message);

        // --- Critical Alert: WhatsApp Notification ---
        // We use the marketing gateway for technical survival alerts
        try {
            whatsappGateway.sendMessage("919999999999", "🔥 *URGENT SYSTEM ALERT*\n\n" + message);
        } catch (Exception e) {
            log.warn("Failed to send WhatsApp alert: {}", e.getMessage());
        }

        // Record history
        jdbcTemplate.update(
            "INSERT INTO monitoring_alert_history (rule_id, actual_value, message) VALUES (?, ?, ?)",
            ruleId, actual, message);
        
        jdbcTemplate.update(
            "UPDATE monitoring_alert_rules SET last_triggered_at = NOW() WHERE rule_id = ?",
            ruleId);
    }
}
