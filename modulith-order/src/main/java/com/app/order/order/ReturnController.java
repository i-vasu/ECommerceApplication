package com.app.order.order;

import com.app.order.entities.ReturnRequest;
import com.app.order.services.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import lombok.Data;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user/orders")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping("/{orderId}/return")
    public ResponseEntity<?> requestReturn(
            @PathVariable Long orderId,
            @RequestBody ReturnRequestPayload payload) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        ReturnRequest request = returnService.requestPartialReturn(
                orderId,
                email,
                payload.getItems(),
                payload.getReason(),
                payload.getRefundType());

        return ResponseEntity.ok(request);
    }

    @Data
    public static class ReturnRequestPayload {
        private Map<Long, Integer> items; // itemId -> quantity
        private String reason;
        private ReturnRequest.RefundType refundType;
    }
}
