package com.app.core.events.user;

import java.time.LocalDateTime;

public class UserDeletedEvent {
    private final Long userId;
    private final LocalDateTime deletedAt;

    public UserDeletedEvent(Long userId) {
        this.userId = userId;
        this.deletedAt = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
