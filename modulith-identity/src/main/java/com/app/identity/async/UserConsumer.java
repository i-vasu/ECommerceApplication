package com.app.identity.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.app.identity.repositories.UserRepo;
import com.app.core.events.ERPNextSyncRequestEvent;
import com.app.core.async.EventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    private final EventProducer eventProducer;
    private final UserRepo userRepo;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        String json = message.getValue();
        if (json == null || json.isEmpty()) {
            return;
        }

        try {
            // Priority 1: Try parsing as structured UserEvent
            try {
                UserEvent event = objectMapper.readValue(json, UserEvent.class);
                if (event != null && event.getUserId() != null) {
                    processEvent(event.getUserId(), event.getEventType());
                    return;
                }
            } catch (Exception e) {
                // Not a UserEvent, try next
            }

            // Priority 2: Try parsing as raw ID (Long)
            try {
                Long userId = Long.parseLong(json);
                processEvent(userId, "USER_REGISTERED");
                return;
            } catch (NumberFormatException e) {
                // Not a raw Long, try next
            }

            // Priority 3: Try parsing as generic JSON to extract ID
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
                if (node.has("userId")) {
                    processEvent(node.get("userId").asLong(),
                            node.has("eventType") ? node.get("eventType").asText() : "USER_REGISTERED");
                } else if (node.has("email")) {
                    userRepo.findByEmail(node.get("email").asText())
                            .ifPresent(u -> processEvent(u.getUserId(), "USER_REGISTERED"));
                }
            } catch (Exception e) {
                log.error("Failed to parse user event payload: {}", json);
            }

        } catch (Exception e) {
            log.error("Uncaught error in UserConsumer: {}", e.getMessage());
        }
    }

    private void processEvent(Long userId, String eventType) {
        if (userId == null)
            return;

        log.info("Processing User Event: {} for User ID: {}", eventType, userId);

        if ("USER_REGISTERED".equals(eventType) || eventType == null) {
            userRepo.findById(userId).ifPresent(user -> {
                log.info("User registered: {}", user.getEmail());

                // Publish ERPNext sync request event (decoupled approach)
                ERPNextSyncRequestEvent syncEvent = new ERPNextSyncRequestEvent(
                        "USER",
                        user.getUserId(),
                        "CREATE");
                eventProducer.publishEvent(syncEvent);
                log.info("Published ERPNext sync request for user: {}", user.getEmail());
            });
        }
    }
}
