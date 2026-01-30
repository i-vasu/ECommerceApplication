package com.app.core.events;

import lombok.Builder;
import lombok.Value;

/**
 * Event published when an autonomous engine detects a system anomaly.
 * Used for cross-module healing and escalations.
 */
@Value
@Builder
public class SystemAnomalyEvent {
    String anomalyType; // e.g. SLA_VIOLATION, BRUTE_FORCE, FRAUD
    String category; // Order, Account, etc.
    String entityId;
    String detail;
    long severity; // 1-10
}
