package com.app.marketing.services;

import com.app.security.entities.User;
import com.app.security.repositories.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Customer Insight & Predictive Analytics Service.
 * Implements RFM (Recency, Frequency, Monetary) analysis for autonomous
 * marketing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerInsightService {

    private final UserRepo userRepo;
    private final JdbcTemplate jdbcTemplate;
    private final WorkflowEngine workflowEngine;

    /**
     * Daily Background Process to update Customer Traits and Segments.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    @Transactional
    public void runDailyInsights() {
        log.info("Starting Daily Customer Insight Analysis...");

        List<User> users = userRepo.findAll();
        for (User user : users) {
            updateUserTraits(user);
        }
    }

    private void updateUserTraits(User user) {
        String email = user.getEmail();

        // 1. Recency: Days since last order
        String recencySql = "SELECT MAX(order_date) FROM orders WHERE email = ?";
        java.sql.Date lastOrderDate = jdbcTemplate.queryForObject(recencySql, java.sql.Date.class, email);
        long recency = lastOrderDate != null
                ? ChronoUnit.DAYS.between(lastOrderDate.toLocalDate(), java.time.LocalDate.now())
                : 999;

        // 2. Frequency: Total successful orders (using SQL instead of OrderRepo)
        String frequencySql = "SELECT COUNT(*) FROM orders WHERE email = ? AND order_status = 'COMPLETED'";
        Long frequencyResult = jdbcTemplate.queryForObject(frequencySql, Long.class, email);
        long frequency = frequencyResult != null ? frequencyResult : 0;

        // 3. Monetary: Total Lifetime Value (LTV)
        String ltvSql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE email = ?";
        double monetary = jdbcTemplate.queryForObject(ltvSql, Double.class, email);

        log.debug("User {}: R={}, F={}, M={}", email, recency, frequency, monetary);

        // Update traits in DB (JSONB)
        String upsertTraits = "INSERT INTO user_traits (email, traits, updated_at) " +
                "VALUES (?, CAST(? AS jsonb), NOW()) " +
                "ON CONFLICT (email) DO UPDATE SET traits = EXCLUDED.traits, updated_at = NOW()";

        String traitsJson = String.format("{\"recency\": %d, \"frequency\": %d, \"monetary\": %.2f}",
                recency, frequency, monetary);

        jdbcTemplate.update(upsertTraits, email, traitsJson);

        // 4. Autonomous Churn Prevention Trigger
        // If a high-value user (Monetary > 1000) hasn't ordered in 30 days
        if (monetary > 1000 && recency > 30 && recency < 35) {
            log.info("Churn Risk Detected: High Value user {} is at risk. Triggering recovery journey.", email);
            workflowEngine.triggerWorkflows("CHURN_RISK", email, Map.of("recency", recency, "monetary", monetary));
        }
    }

    public Map<String, Object> getSegmentStats() {
        String sql = "SELECT COUNT(*) as count, " +
                "CASE " +
                "  WHEN (traits->>'monetary')::float > 5000 THEN 'VIP' " +
                "  WHEN (traits->>'recency')::int > 60 THEN 'CHURNING' " +
                "  WHEN (traits->>'frequency')::int > 10 THEN 'LOYAL' " +
                "  ELSE 'NORMAL' END as segment " +
                "FROM user_traits GROUP BY segment";

        return jdbcTemplate.queryForList(sql).stream()
                .collect(java.util.stream.Collectors.toMap(
                        m -> (String) m.get("segment"),
                        m -> m.get("count")));
    }
}
