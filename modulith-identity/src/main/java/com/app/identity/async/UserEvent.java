package com.app.identity.async;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Internal event structure for user-related messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private Long userId;
    private String eventType;
    private String tenantId;
}
