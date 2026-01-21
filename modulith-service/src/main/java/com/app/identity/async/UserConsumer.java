package com.app.identity.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.app.identity.entities.User;
import com.app.identity.repositories.UserRepo;
import com.app.order.services.ERPNextService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class UserConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    @Autowired
    private ERPNextService erpNextService;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ObjectMapper objectMapper;

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
                processEvent(userId, "USER_REGISTERED"); // Defaulting to registration if only ID provided
                return;
            } catch (NumberFormatException e) {
                // Not a raw Long, try next
            }

            // Priority 3: Try parsing as User entity or DTO to extract ID
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
                if (node.has("userId")) {
                    processEvent(node.get("userId").asLong(),
                            node.has("eventType") ? node.get("eventType").asText() : "USER_REGISTERED");
                } else if (node.has("email")) {
                    // Fallback to email lookup if ID is missing in DTO
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
                log.info("Syncing User to ERPNext: {}", user.getEmail());
                erpNextService.createCustomer(user);
            });
        }
        // Handle other event types here (USER_UPDATED, etc.)
    }

    // Internal Event Structure
    public static class UserEvent {
        private Long userId;
        private String eventType;

        public UserEvent() {
        }

        public UserEvent(Long userId, String eventType) {
            this.userId = userId;
            this.eventType = eventType;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }
    }
}
