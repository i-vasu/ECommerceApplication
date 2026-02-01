package com.app.governance.services;

import com.app.governance.audit.OperationalAudit;
import com.app.governance.audit.OperationalAuditRepo;
import com.app.governance.rules.RuleEngineService;
import com.app.governance.states.AccountEvent;
import com.app.governance.states.OperationalStateMachineService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autonomous Anomaly Detection & Self-Healing Service.
 * Scans operational audits for patterns and triggers corrective state
 * transitions.
 */
@Service
public class AnomalyDetectionService {

    private static final Logger log = LogManager.getLogger(AnomalyDetectionService.class);
    private final OperationalAuditRepo auditRepo;
    private final com.app.governance.rules.RuleEngineService ruleEngine;
    private final OperationalStateMachineService stateMachineService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AnomalyDetectionService(
            OperationalAuditRepo auditRepo,
            RuleEngineService ruleEngine,
            OperationalStateMachineService stateMachineService,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.auditRepo = auditRepo;
        this.ruleEngine = ruleEngine;
        this.stateMachineService = stateMachineService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 60000) // Every minute
    public void detectAndHeal() {
        log.debug("Running Autonomous Anomaly Detection...");

        // 1. Detect account brute-force/failure patterns
        detectAuthAnomalies();

        // 2. Detect high-frequency order failures
        detectOrderAnomalies();
    }

    private void detectAuthAnomalies() {
        LocalDateTime window = LocalDateTime.now().minusMinutes(5);
        List<OperationalAudit> recentAudits = auditRepo.findByTimestampAfter(window);

        // Group by Entity (User ID)
        Map<String, Integer> failureCounts = new HashMap<>();
        for (OperationalAudit audit : recentAudits) {
            if ("Account".equals(audit.getCategory()) && !audit.isSuccess()) {
                failureCounts.put(audit.getEntityId(), failureCounts.getOrDefault(audit.getEntityId(), 0) + 1);
            }
        }

        // Apply SpEL Rule for "Suspension Threshold"
        failureCounts.forEach((userId, count) -> {
            Map<String, Object> context = new HashMap<>();
            context.put("failureCount", count);

            // Rule: If more than 5 failures in 5 minutes, suspend
            if (ruleEngine.evaluate("failureCount >= 5", context)) {
                log.error("Anomaly Detected: High failure rate for User {}. Triggering Auto-Suspension.", userId);
                stateMachineService.triggerAccountEvent(Long.parseLong(userId), AccountEvent.SUSPEND);

                // Log the healing action
                auditRepo.save(OperationalAudit.builder()
                        .type("SELF_HEALING")
                        .category("System")
                        .detail("Auto-suspended user " + userId + " due to " + count + " failed attempts.")
                        .success(true)
                        .build());
            }
        });
    }

    private void detectOrderAnomalies() {
        LocalDateTime window = LocalDateTime.now().minusDays(3); // Look back 3 days
        List<OperationalAudit> audits = auditRepo.findByTimestampAfter(window);

        Map<String, OperationalAudit> latestStatus = new HashMap<>();

        // 1. Find the latest status for each Order
        for (OperationalAudit audit : audits) {
            if ("Order".equals(audit.getCategory()) && "STATE_TRANSITION".equals(audit.getType())) {
                String id = audit.getEntityId();
                if (!latestStatus.containsKey(id)
                        || audit.getTimestamp().isAfter(latestStatus.get(id).getTimestamp())) {
                    latestStatus.put(id, audit);
                }
            }
        }

        // 2. Check for SLA Violations (Stalled Processes)
        latestStatus.forEach((id, audit) -> {
            String detail = audit.getDetail(); // "PAID --[SHIP]--> SHIPPED"
            String currentState = extractTargetState(detail);

            if (!isTerminalState(currentState)) {
                long hoursStalled = java.time.Duration.between(audit.getTimestamp(), LocalDateTime.now()).toHours();

                // Rule: If stalled for > 24 hours in non-terminal state
                Map<String, Object> context = new HashMap<>();
                context.put("hoursStalled", hoursStalled);
                context.put("currentState", currentState);

                if (ruleEngine.evaluate("hoursStalled >= 24", context)) {
                    log.warn("SLA Violation: Order {} stuck in {} for {} hours.", id, currentState, hoursStalled);

                    auditRepo.save(OperationalAudit.builder()
                            .type("SLA_VIOLATION")
                            .category("Order")
                            .entityId(id)
                            .detail("Stuck in " + currentState + " for " + hoursStalled + "h")
                            .success(false)
                            .result("ALERT")
                            .build());

                    // Trigger Cross-Module Escalation
                    eventPublisher.publishEvent(com.app.core.events.SystemAnomalyEvent.builder()
                            .anomalyType("SLA_VIOLATION")
                            .category("Order")
                            .entityId(id)
                            .detail("Order stuck for " + hoursStalled + "h in state: " + currentState)
                            .severity(5)
                            .build());
                }
            }
        });
    }

    private String extractTargetState(String detail) {
        if (detail == null || !detail.contains("-->"))
            return "UNKNOWN";
        return detail.substring(detail.lastIndexOf("-->") + 3).trim();
    }

    private boolean isTerminalState(String state) {
        return List.of("COMPLETED", "CANCELLED", "DELIVERED", "RETURNED", "REFUNDED", "FAILED")
                .contains(state.toUpperCase());
    }
}
