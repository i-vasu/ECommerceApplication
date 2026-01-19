package com.app.inventory;

import com.app.admin.services.ERPNextProductSyncService;
import com.app.search.services.ProductDataFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("/api/v1/inventory/webhooks")
public class ERPNextWebhookController implements InventoryWebhookApi {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ERPNextWebhookController.class);

    @Value("${erpnext.webhook.secret:}")
    private String webhookSecret;

    @Autowired
    private ERPNextProductSyncService syncService;

    @Autowired
    private ProductDataFlowService dataFlowService;

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
            // ERPNext Webhook Payload structure usually has 'doc'
            Object docObj = payload.get("doc");
            if (docObj instanceof Map<?, ?> doc) {
                // Safely convert to Map<String, Object> if needed or just use as Map<?, ?>
                // The unchecked cast is suppressed as we've checked it's a Map,
                // and the generic types are assumed based on ERPNext payload structure.
                @SuppressWarnings("unchecked")
                Map<String, Object> typedDoc = (Map<String, Object>) doc;
                log.info("Processing single item from webhook: {}", typedDoc.get("item_code"));
                dataFlowService.processItemWithMedia(typedDoc);
            } else {
                log.info("Processing direct payload as item");
                dataFlowService.processItemWithMedia(payload);
            }
            return ResponseEntity.ok("Processed");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.internalServerError().body("Error");
        }
    }
}
