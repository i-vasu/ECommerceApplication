package com.app.erp_sync.gateway;

import com.app.core.events.ERPItemSyncRequestedEvent;
import com.app.core.multitenancy.ERPNextCredentialProvider;
import com.app.core.utils.HmacUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("ERPNextSyncWebhookController")
@RequestMapping("/api/webhooks/erpnext")
public class ERPNextWebhookController implements InventoryWebhookApi {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ERPNextWebhookController.class);

    @Autowired
    private ERPNextService erpNextService;

    @Autowired
    private ERPNextCredentialProvider credentialProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${erpnext.webhook.secret:}")
    private String webhookSecret;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public ResponseEntity<String> handleItemWebhook(
            @RequestHeader(value = "X-ERPNext-Webhook-Secret", required = false) String secret,
            @RequestBody Map<String, Object> payload) {

        if (webhookSecret != null && !webhookSecret.isEmpty() && !webhookSecret.equals(secret)) {
            log.warn("Invalid webhook secret received");
            return ResponseEntity.status(403).body("Invalid Secret");
        }

        log.info("Received Item Webhook from ERPNext: {}", payload);

        try {
            Map<String, Object> itemData;
            Object docObj = payload.get("doc");
            if (docObj instanceof Map<?, ?> doc) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedDoc = (Map<String, Object>) doc;
                itemData = typedDoc;
            } else {
                itemData = payload;
            }

            log.info("ERP-Sync: Publishing sync request for item: {}", itemData.get("item_code"));

            // Decoupled: Publish event instead of calling discovery service directly
            eventPublisher.publishEvent(new ERPItemSyncRequestedEvent(itemData));

            return ResponseEntity.ok("Sync Request Received");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.internalServerError().body("Error");
        }
    }

    @PostMapping("/order-status")
    public ResponseEntity<String> handleOrderStatusWebhook(
            @RequestHeader(value = "X-ERPNext-Signature", required = false) String signature,
            HttpServletRequest request) {

        try {
            String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            String secret = credentialProvider.getWebhookSecret();

            if (secret != null && !HmacUtils.verifyHmac(payload, signature, secret)) {
                log.error("Invalid HMAC signature for ERPNext webhook");
                return ResponseEntity.status(401).body("Invalid signature");
            }

            JsonNode root = objectMapper.readTree(payload);
            String orderName = root.path("name").asText();
            String status = root.path("status").asText();

            log.info("Received ERPNext webhook for order: {} with status: {}", orderName, status);

            erpNextService.handleWebhookStatusUpdate(orderName, status, root);

            return ResponseEntity.ok("Webhook processed");
        } catch (IOException e) {
            log.error("Error processing ERPNext webhook", e);
            return ResponseEntity.badRequest().body("Error processing payload");
        }
    }
}
