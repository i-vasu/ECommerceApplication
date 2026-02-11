package com.app.core.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProcessedEventRepo extends JpaRepository<ProcessedEvent, String> {
    boolean existsByEventIdAndConsumerId(String eventId, String consumerId);
    void deleteByProcessedAtBefore(LocalDateTime cutoff);
}
