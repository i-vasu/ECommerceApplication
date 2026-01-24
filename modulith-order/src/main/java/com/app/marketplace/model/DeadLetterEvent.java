package com.app.marketplace.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_events")
public class DeadLetterEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String channel;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String errorReason;

    private LocalDateTime failedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    @PrePersist
    protected void onCreate() {
        failedAt = LocalDateTime.now();
    }
}
