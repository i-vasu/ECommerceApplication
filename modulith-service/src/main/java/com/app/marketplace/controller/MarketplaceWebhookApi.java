package com.app.marketplace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Tag(name = "Marketplace Webhooks", description = "Webhooks for Marketplace Integrations (Amazon, Flipkart, ONDC)")
public interface MarketplaceWebhookApi {

    @Operation(summary = "Handle Marketplace Webhook", description = "Processes incoming webhooks from external marketplaces")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processed"),
            @ApiResponse(responseCode = "400", description = "Unsupported channel or Duplicate event"),
            @ApiResponse(responseCode = "401", description = "Invalid Signature"),
            @ApiResponse(responseCode = "500", description = "Processing error")
    })
    @PostMapping("/{channel}")
    ResponseEntity<String> handleWebhook(
            @Parameter(description = "Channel name (amazon, flipkart, ondc)") @PathVariable String channel,
            @RequestBody Map<String, Object> payload,
            @Parameter(description = "Signature for verification") @RequestHeader(value = "X-Marketplace-Signature", required = false) String signature);
}
