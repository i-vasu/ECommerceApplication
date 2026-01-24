package com.app.marketplace.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.app.marketplace.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/marketplace/webhooks")
public class WebhookController implements MarketplaceWebhookApi {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Override
    public ResponseEntity<String> handleWebhook(
            @PathVariable String channel,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Marketplace-Signature", required = false) String signature) {
        try {
            String result = webhookService.processWebhook(channel, payload, signature);
            if ("Duplicate event".equals(result)) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Service already logged and DLQ'd
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

}
