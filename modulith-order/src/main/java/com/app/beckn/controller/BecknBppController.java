package com.app.beckn.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/beckn")
@Tag(name = "Beckn Protocol BPP", description = "Beckn Protocol Endpoints for ONDC and other open networks")
@Slf4j
@RequiredArgsConstructor
public class BecknBppController {

    private final com.app.beckn.service.BecknDiscoveryService discoveryService;
    private final com.app.beckn.service.BecknTransactionService transactionService;

    @PostMapping("/search")
    @Operation(summary = "Handle Search Intent", description = "Responds to catalog search requests from the Beckn network")
    public ResponseEntity<Map<String, Object>> onSearch(@RequestBody Map<String, Object> becknPayload) {
        log.info("Received Beckn Search request: {}", becknPayload);
        Map<String, Object> response = discoveryService.processSearch(becknPayload);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/select")
    public ResponseEntity<Map<String, Object>> onSelect(@RequestBody Map<String, Object> becknPayload) {
        log.info("Received Beckn Select request: {}", becknPayload);
        return ResponseEntity.ok(transactionService.processSelect(becknPayload));
    }

    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> onInit(@RequestBody Map<String, Object> becknPayload) {
        log.info("Received Beckn Init request: {}", becknPayload);
        return ResponseEntity.ok(transactionService.processInit(becknPayload));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> onConfirm(@RequestBody Map<String, Object> becknPayload) {
        log.info("Received Beckn Confirm request: {}", becknPayload);
        return ResponseEntity.ok(transactionService.processConfirm(becknPayload));
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> onStatus(@RequestBody Map<String, Object> becknPayload) {
        log.info("Received Beckn Status request: {}", becknPayload);
        return ResponseEntity.ok(Map.of("message", "Status request received"));
    }

    @PostMapping("/track")
    public ResponseEntity<Map<String, Object>> onTrack(@RequestBody Map<String, Object> becknPayload) {
        log.info("Received Beckn Track request: {}", becknPayload);
        return ResponseEntity.ok(Map.of("message", "Track request received"));
    }
}
