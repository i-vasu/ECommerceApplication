package com.app.core.events;

import java.io.Serializable;
import java.util.List;

/**
 * Event published when a return is officially approved by admin.
 * Listened by: modulith-order, modulith-finance, modulith-erp-sync
 */
public record ReturnApprovedEvent(
        Long requestId,
        Long orderId,
        String email,
        double refundAmount,
        String refundType,
        List<ApprovedReturnItem> items) implements Serializable {
    public record ApprovedReturnItem(Long orderItemId, String itemCode, Integer quantity) {
    }
}
