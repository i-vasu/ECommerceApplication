package com.app.support.domain;

import com.app.support.entities.ReturnRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user/orders")
@Tag(name = "Returns", description = "Customer Order Return Management APIs")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @Operation(summary = "Request Order Return", description = "Submits a request to return one or more items from a delivered order.")
    @ApiResponse(responseCode = "200", description = "Return request accepted")
    @ApiResponse(responseCode = "400", description = "Invalid items or return window expired")
    @PostMapping("/{orderId}/return")
    // Note: ideally we'd have a check if the user OWNS the orderId.
    // Assuming returnService.requestPartialReturn handles ownership check via email lookup.
    public ResponseEntity<?> requestReturn(
            @Parameter(description = "ID of the order to return items from") @PathVariable Long orderId,
            @RequestBody ReturnRequestPayload payload) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        returnService.requestPartialReturn(
                orderId,
                email,
                payload.getItems(),
                payload.getReason(),
                ReturnRequest.RefundType.valueOf(payload.getRefundType()));

        return ResponseEntity.ok(java.util.Map.of("message", "Return request submitted and is being processed"));
    }

    @Operation(summary = "List My Returns", description = "Retrieves all return requests for the currently authenticated user.")
    @ApiResponse(responseCode = "200", description = "Returns retrieved successfully")
    @GetMapping("/returns")
    public ResponseEntity<java.util.List<ReturnRequest>> getUserReturns() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(returnService.getUserReturns(email));
    }

    public static class ReturnRequestPayload {
        private Map<Long, Integer> items;
        private String reason;
        private String refundType;

        public Map<Long, Integer> getItems() { return items; }
        public void setItems(Map<Long, Integer> items) { this.items = items; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getRefundType() { return refundType; }
        public void setRefundType(String refundType) { this.refundType = refundType; }
    }
}
