package com.app.core.events;

import com.app.core.async.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Autonomous Outbox Processor.
 * Ensures "At-Least-Once" delivery of events by polling the outbox table and
 * publishing to external streams.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxRepo outboxRepo;
    private final EventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> pendingEvents = outboxRepo.findByProcessedFalseOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty())
            return;

        log.debug("Processing {} pending outbox events...", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Publish to Redis Stream / External Bus
                eventProducer.publish(event.getAggregateType() + "_events", event.getPayload());

                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepo.save(event);
            } catch (Exception e) {
                log.error("Failed to process outbox event {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                outboxRepo.save(event);
            }
        }
    }
}
