package com.app.security.async;

/**
 * Internal event structure for user-related messages.
 */
public class UserEvent {
    private Long userId;
    private String eventType;
    private String tenantId;

    public UserEvent() {}
    public UserEvent(Long userId, String eventType, String tenantId) {
        this.userId = userId;
        this.eventType = eventType;
        this.tenantId = tenantId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
