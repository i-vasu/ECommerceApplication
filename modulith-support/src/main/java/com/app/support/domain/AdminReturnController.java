package com.app.support.domain;

import com.app.support.entities.ReturnRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for managing return requests.
 * Exposes existing ReturnService methods that had no REST API.
 */
@RestController
@RequestMapping("/api/v1/admin/returns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReturnController {

    private final ReturnService returnService;

    @GetMapping
    public ResponseEntity<Page<ReturnRequest>> getAllReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(returnService.getAllReturns(page, size));
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<Map<String, String>> approveReturn(@PathVariable Long requestId) {
        returnService.approveReturn(requestId);
        return ResponseEntity.ok(Map.of("message", "Return request #" + requestId + " approved"));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<Map<String, String>> rejectReturn(
            @PathVariable Long requestId,
            @RequestParam String reason) {
        returnService.rejectReturn(requestId, reason);
        return ResponseEntity.ok(Map.of("message", "Return request #" + requestId + " rejected"));
    }

    @PostMapping("/{requestId}/receive")
    public ResponseEntity<Map<String, String>> markAsReceived(
            @PathVariable Long requestId,
            @RequestParam(required = false) String comments) {
        returnService.markAsReceived(requestId, comments);
        return ResponseEntity.ok(Map.of("message", "Return request #" + requestId + " marked as received and completed"));
    }
}
