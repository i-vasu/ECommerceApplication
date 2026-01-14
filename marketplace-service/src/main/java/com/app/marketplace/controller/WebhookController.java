package com.app.marketplace.controller;

import com.app.marketplace.adapter.AmazonAdapter;
import com.app.marketplace.adapter.FlipkartAdapter;
import com.app.marketplace.adapter.MarketplaceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final AmazonAdapter amazonAdapter;
    private final FlipkartAdapter flipkartAdapter;
    private final com.app.marketplace.repository.RawEventRepository rawEventRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @PostMapping("/{channel}")
    public ResponseEntity<String> handleWebhook(@PathVariable String channel,
            @RequestBody Map<String, Object> payload) {
        log.info("Received webhook for channel: {}", channel);

        // 0. Idempotency Check
        String eventId = extractEventId(payload);
        String idempotencyKey = "webhook:" + channel + ":" + eventId;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(idempotencyKey))) {
            log.info("Duplicate webhook event detected: {}", idempotencyKey);
            return ResponseEntity.ok("Duplicate event");
        }

        redisTemplate.opsForValue().set(idempotencyKey, "RECEIVED", java.time.Duration.ofHours(24));

        // 1. Persistence
        saveRawEvent(channel, payload);

        MarketplaceAdapter adapter = getAdapter(channel);
        if (adapter == null) {
            return ResponseEntity.badRequest().body("Unsupported channel: " + channel);
        }

        try {
            adapter.handleWebhook(payload);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    private String extractEventId(Map<String, Object> payload) {
        if (payload.containsKey("id"))
            return payload.get("id").toString();
        if (payload.containsKey("eventId"))
            return payload.get("eventId").toString();
        if (payload.containsKey("orderId"))
            return payload.get("orderId").toString();

        try {
            return java.security.MessageDigest.getInstance("MD5")
                    .digest(objectMapper.writeValueAsBytes(payload)).toString();
        } catch (Exception e) {
            return String.valueOf(payload.hashCode());
        }
    }

    private void saveRawEvent(String channel, Map<String, Object> payload) {
        try {
            com.app.marketplace.model.RawEvent event = new com.app.marketplace.model.RawEvent();
            event.setChannel(channel);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus("RECEIVED");
            rawEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to save raw event", e);
            // We might want to continue or fail depending on strictness.
            // LLD says "Persistence" is step 4, implying flow should continue or maybe fail
            // if critical.
            // For now, logging error.
        }
    }

    private MarketplaceAdapter getAdapter(String channel) {
        if ("amazon".equalsIgnoreCase(channel)) {
            return amazonAdapter;
        } else if ("flipkart".equalsIgnoreCase(channel)) {
            return flipkartAdapter;
        }
        return null;
    }
}
