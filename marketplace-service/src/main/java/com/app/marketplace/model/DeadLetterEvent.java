package com.app.marketplace.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_events")
@Data
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

    @PrePersist
    protected void onCreate() {
        failedAt = LocalDateTime.now();
    }
}
