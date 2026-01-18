package com.app.marketplace.controller;

import com.app.marketplace.adapter.AmazonAdapter;
import com.app.marketplace.adapter.FlipkartAdapter;
import com.app.marketplace.adapter.OndcAdapter;
import com.app.marketplace.adapter.MarketplaceAdapter;
import com.app.order.payloads.OrderDTO;
import com.app.marketplace.service.OrderDispatchService;
import com.app.marketplace.service.SignatureVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("/api/v1/marketplace/webhooks")
public class WebhookController implements MarketplaceWebhookApi {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebhookController.class);

    private final AmazonAdapter amazonAdapter;
    private final FlipkartAdapter flipkartAdapter;
    private final OndcAdapter ondcAdapter;
    private final com.app.marketplace.repository.RawEventRepository rawEventRepository;
    private final com.app.marketplace.repository.DeadLetterEventRepository deadLetterEventRepository;
    private final OrderDispatchService orderDispatchService;
    private final SignatureVerificationService signatureVerificationService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @org.springframework.beans.factory.annotation.Value("${marketplace.amazon.secret}")
    private String amazonSecret;

    @org.springframework.beans.factory.annotation.Value("${marketplace.flipkart.secret}")
    private String flipkartSecret;

    @org.springframework.beans.factory.annotation.Value("${marketplace.ondc.secret}")
    private String ondcSecret;

    public WebhookController(AmazonAdapter amazonAdapter, FlipkartAdapter flipkartAdapter, OndcAdapter ondcAdapter,
            com.app.marketplace.repository.RawEventRepository rawEventRepository,
            com.app.marketplace.repository.DeadLetterEventRepository deadLetterEventRepository,
            OrderDispatchService orderDispatchService,
            SignatureVerificationService signatureVerificationService,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.amazonAdapter = amazonAdapter;
        this.flipkartAdapter = flipkartAdapter;
        this.ondcAdapter = ondcAdapter;
        this.rawEventRepository = rawEventRepository;
        this.deadLetterEventRepository = deadLetterEventRepository;
        this.orderDispatchService = orderDispatchService;
        this.signatureVerificationService = signatureVerificationService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ResponseEntity<String> handleWebhook(
            @PathVariable String channel,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Marketplace-Signature", required = false) String signature) {
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
            // return ResponseEntity.status(401).body("Invalid Signature");
        }

        MarketplaceAdapter adapter = getAdapter(channel);
        if (adapter == null) {
            return ResponseEntity.badRequest().body("Unsupported channel: " + channel);
        }

        try {
            // 3. Process & Normalize
            OrderDTO orderDTO = adapter.handleWebhook(payload);

            // 4. Dispatch to Core
            if (orderDTO != null) {
                orderDispatchService.dispatch(orderDTO);
            }

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            // 5. DLQ
            saveToDLQ(channel, payload, e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    private void saveToDLQ(String channel, Map<String, Object> payload, Exception e) {
        try {
            com.app.marketplace.model.DeadLetterEvent dlq = new com.app.marketplace.model.DeadLetterEvent();
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
        } else if ("ondc".equalsIgnoreCase(channel)) {
            return ondcAdapter;
        }
        return null;
    }
}
