package com.app.core.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventIdempotencyService {

    private final ProcessedEventRepo processedEventRepo;

    /**
     * Checks if an event has already been processed by a specific consumer.
     * Uses REQUIRES_NEW to ensure the check and insert are committed independently
     * of the main transaction if needed, or we can rely on unique constraint exception.
     *
     * Ideally, this should be part of the atomic transaction of the consumer.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean isEventProcessed(String eventId, String consumerId, String eventType) {
        if (processedEventRepo.existsByEventIdAndConsumerId(eventId, consumerId)) {
            log.warn("Event {} already processed by consumer {}. Skipping.", eventId, consumerId);
            return true;
        }
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void markEventAsProcessed(String eventId, String consumerId, String eventType) {
        try {
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventId(eventId)
                    .consumerId(consumerId)
                    .eventType(eventType)
                    .processedAt(LocalDateTime.now())
                    .build();
            processedEventRepo.save(processedEvent);
        } catch (Exception e) {
            // Concurrent insertion might cause unique constraint violation
            log.warn("Failed to mark event {} as processed (likely duplicate): {}", eventId, e.getMessage());
        }
    }
}
