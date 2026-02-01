package com.app.erp_sync.gateway;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@Tag(name = "ERP Sync Webhooks", description = "Webhooks for ERPNext Sync")
public interface InventoryWebhookApi {

    @Operation(summary = "Handle Item Webhook", description = "Processes item updates from ERPNext webhook")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processed"),
            @ApiResponse(responseCode = "403", description = "Invalid Secret"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping("/api/v1/erpnext/item")
    ResponseEntity<String> handleItemWebhook(
            @RequestHeader(value = "X-ERPNext-Webhook-Secret", required = false) String secret,
            @RequestBody Map<String, Object> payload);
}
