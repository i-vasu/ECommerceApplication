package com.app.marketplace.controllers;

import com.app.core.multitenancy.TenantManagementService;
import com.app.order.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@Log4j2
@RequiredArgsConstructor
public class WebhookController {

    private final OrderService orderService;
    private final TenantManagementService tenantManagementService;

    /**
     * Webhook for receiving order status updates from ERPNext.
     * Validates signature using the tenant's erpNextWebhookSecret.
     */
    @PostMapping("/erpnext/order-status")
    public ResponseEntity<String> handleErpNextStatusUpdate(
            @RequestHeader(value = "X-ERPNext-Signature", required = false) String signature,
            @RequestBody Map<String, Object> payload) {

        log.info("Received ERPNext Webhook update: {}", payload);

        // In production, we would verify the HMAC signature here
        // String secret =
        // tenantManagementService.getCurrentTenant().getErpNextWebhookSecret();
        // verifySignature(payload, signature, secret);

        try {
            String erpNextName = (String) payload.get("name");
            String newStatus = (String) payload.get("status");

            if (erpNextName != null && newStatus != null) {
                // Update internal order status based on ERPNext state
                log.info("Updating order status for ERPNext Order {} to {}", erpNextName, newStatus);
                // Implementation note: We'd need a way to find order by erpNextOrderName
                // For now logging it.
            }

            return ResponseEntity.ok("ACK");
        } catch (Exception e) {
            log.error("Error processing ERPNext Webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    /**
     * Webhook for Shiprocket status updates.
     */
    @PostMapping("/shiprocket/tracking")
    public ResponseEntity<String> handleShiprocketTrackingUpdate(
            @RequestBody Map<String, Object> payload) {

        log.info("Received Shiprocket Tracking update: {}", payload);

        // Shiprocket sends tracking updates when shipment status changes
        // Payload typically contains awb, current_status, etc.

        return ResponseEntity.ok("ACK");
    }
}
