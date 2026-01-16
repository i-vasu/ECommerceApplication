package com.app.inventory;

import com.app.services.ERPNextProductSyncService;
import com.app.services.ProductDataFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@Slf4j
public class ERPNextWebhookController {

    @Value("${erpnext.webhook.secret:}")
    private String webhookSecret;

    @Autowired
    private ERPNextProductSyncService syncService;

    @Autowired
    private ProductDataFlowService dataFlowService;

    @PostMapping("/erpnext/item")
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
            if (payload.containsKey("doc")) {
                Map<String, Object> doc = (Map<String, Object>) payload.get("doc");
                syncService.syncItems(); // For now, trigger full sync to be safe, or optimize to process single item
                // Ideally: processSingleItem(doc);
            } else {
                // Direct payload
                // processSingleItem(payload);
                syncService.syncItems();
            }
            return ResponseEntity.ok("Processed");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.internalServerError().body("Error");
        }
    }
}
