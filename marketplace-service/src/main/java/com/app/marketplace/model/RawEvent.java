package com.app.marketplace.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_events")
@Data
public class RawEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String channel;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private String status; // RECEIVED, PROCESSED, FAILED

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
