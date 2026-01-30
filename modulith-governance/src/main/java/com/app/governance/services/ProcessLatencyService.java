package com.app.governance.services;

import com.app.governance.audit.OperationalAudit;
import com.app.governance.audit.OperationalAuditRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business Process Latency Monitoring Service.
 * Tracks the "Velocity" of state transitions to detect abandoned or stuck
 * business processes.
 */
@Service
public class ProcessLatencyService {

    private static final Logger log = LogManager.getLogger(ProcessLatencyService.class);
    private final OperationalAuditRepo auditRepo;

    public ProcessLatencyService(OperationalAuditRepo auditRepo) {
        this.auditRepo = auditRepo;
    }

    public Map<String, Double> calculateAverageLeadTime(String category, LocalDateTime since) {
        List<OperationalAudit> audits = auditRepo.findByTimestampAfter(since);

        // Filter by category and group by ID to find intervals
        Map<String, LocalDateTime> firstSeen = new HashMap<>();
        Map<String, LocalDateTime> lastSeen = new HashMap<>();

        for (OperationalAudit audit : audits) {
            if (category.equalsIgnoreCase(audit.getCategory()) && audit.getEntityId() != null) {
                String id = audit.getEntityId();
                if (!firstSeen.containsKey(id) || audit.getTimestamp().isBefore(firstSeen.get(id))) {
                    firstSeen.put(id, audit.getTimestamp());
                }
                if (!lastSeen.containsKey(id) || audit.getTimestamp().isAfter(lastSeen.get(id))) {
                    lastSeen.put(id, audit.getTimestamp());
                }
            }
        }

        double totalMinutes = 0;
        int count = 0;

        for (String id : firstSeen.keySet()) {
            LocalDateTime start = firstSeen.get(id);
            LocalDateTime end = lastSeen.get(id);
            if (!start.equals(end)) {
                totalMinutes += ChronoUnit.MINUTES.between(start, end);
                count++;
            }
        }

        Map<String, Double> result = new HashMap<>();
        result.put("averageMinutes", count > 0 ? totalMinutes / count : 0.0);
        result.put("entityCount", (double) count);
        return result;
    }
}
