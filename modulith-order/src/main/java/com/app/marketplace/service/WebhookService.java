package com.app.marketplace.service;

import com.app.marketplace.adapter.AmazonAdapter;
import com.app.marketplace.adapter.FlipkartAdapter;
import com.app.marketplace.adapter.MarketplaceAdapter;
import com.app.marketplace.adapter.OndcAdapter;
import com.app.marketplace.model.DeadLetterEvent;
import com.app.marketplace.model.RawEvent;
import com.app.marketplace.repository.DeadLetterEventRepository;
import com.app.marketplace.repository.RawEventRepository;
import com.app.order.payloads.OrderDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final AmazonAdapter amazonAdapter;
    private final FlipkartAdapter flipkartAdapter;
    private final OndcAdapter ondcAdapter;
    private final RawEventRepository rawEventRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final OrderDispatchService orderDispatchService;
    private final SignatureVerificationService signatureVerificationService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${marketplace.amazon.secret}")
    private String amazonSecret;

    @Value("${marketplace.flipkart.secret}")
    private String flipkartSecret;

    @Value("${marketplace.ondc.secret}")
    private String ondcSecret;

    public String processWebhook(String channel, Map<String, Object> payload, String signature) {
        log.info("Received webhook for channel: {}", channel);

        // 0. Idempotency Check
        String eventId = extractEventId(payload);
        String idempotencyKey = "webhook:" + channel + ":" + eventId;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(idempotencyKey))) {
            log.info("Duplicate webhook event detected: {}", idempotencyKey);
            return "Duplicate event";
        }

        redisTemplate.opsForValue().set(idempotencyKey, "RECEIVED", Duration.ofHours(24));

        // 1. Persistence
        saveRawEvent(channel, payload);

        // 2. Validate Signature
        String secret = "mock";
        if ("amazon".equalsIgnoreCase(channel))
            secret = amazonSecret;
        else if ("flipkart".equalsIgnoreCase(channel))
            secret = flipkartSecret;
        else if ("ondc".equalsIgnoreCase(channel))
            secret = ondcSecret;

        if (signature != null
                && !signatureVerificationService.verifySignature(payload.toString(), signature, secret)) {
            log.warn("Signature verification failed for {}", channel);
            // In Production, UNCOMMENT the line below to reject invalid requests
            // throw new RuntimeException("Invalid Signature");
        }

        MarketplaceAdapter adapter = getAdapter(channel);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported channel: " + channel);
        }

        try {
            // 3. Process & Normalize
            OrderDTO orderDTO = adapter.handleWebhook(payload);

            // 4. Dispatch to Core
            if (orderDTO != null) {
                orderDispatchService.dispatch(orderDTO);
            }

            return "Webhook processed successfully";
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            // 5. DLQ
            saveToDLQ(channel, payload, e);
            throw new RuntimeException("Error processing webhook", e);
        }
    }

    private void saveToDLQ(String channel, Map<String, Object> payload, Exception e) {
        try {
            DeadLetterEvent dlq = new DeadLetterEvent();
            dlq.setChannel(channel);
            dlq.setPayload(objectMapper.writeValueAsString(payload));
            dlq.setErrorReason(e.getMessage());
            deadLetterEventRepository.save(dlq);
        } catch (Exception ex) {
            log.error("Failed to save to DLQ", ex);
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
            return MessageDigest.getInstance("MD5")
                    .digest(objectMapper.writeValueAsBytes(payload)).toString();
        } catch (Exception e) {
            return String.valueOf(payload.hashCode());
        }
    }

    private void saveRawEvent(String channel, Map<String, Object> payload) {
        try {
            RawEvent event = new RawEvent();
            event.setChannel(channel);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus("RECEIVED");
            rawEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to save raw event", e);
        }
    }

    private MarketplaceAdapter getAdapter(String channel) {
        if ("amazon".equalsIgnoreCase(channel)) {
            return amazonAdapter;
        } else if ("flipkart".equalsIgnoreCase(channel)) {
            return flipkartAdapter;
        } else if ("ondc".equalsIgnoreCase(channel)) {
            return ondcAdapter;
        }
        return null;
    }
}
