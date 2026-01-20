package com.app.identity.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.app.identity.entities.User;
import com.app.identity.repositories.UserRepo;
import com.app.order.services.ERPNextService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.order.payloads.UserDTO;

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
        try {
            String json = message.getValue();
            // We publish User Entity or DTO? Let's assume we publish userId or DTO.
            // Best practice: Publish ID, fetch latest state. Or Publish DTO.
            // Given UserService had "User registeredUser" (Entity). 
            // Publishing ID is safer for consistency.
            
            // Wait, Producer publishes generic Object.
            // If we publish User Entity, it might have issues deserializing lazy collections.
            // Better to publish a "UserRegisteredEvent" with userId.
            
            // For now, let's assume we publish a simple event or just the User ID.
            // Let's align with OrderProducer which sends ID.
            // But EventProducer sends Object.
            
            UserEvent event = objectMapper.readValue(json, UserEvent.class);
            
            if ("USER_REGISTERED".equals(event.getEventType())) {
                User user = userRepo.findById(event.getUserId()).orElse(null);
                if (user != null) {
                    System.out.println("Processing User Registration for ERP: " + user.getEmail());
                    erpNextService.createCustomer(user);
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing user event: " + e.getMessage());
        }
    }
    
    // Internal Event Structure
    public static class UserEvent {
        private Long userId;
        private String eventType;
        
        public UserEvent() {}
        public UserEvent(Long userId, String eventType) { this.userId = userId; this.eventType = eventType; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
    }
}
