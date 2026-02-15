package com.app.core.events;

import java.util.List;

public record ReturnPickupInitiatedEvent(
    Long requestId,
    Long orderId,
    String userEmail,
    String reason,
    List<ApprovedReturnItem> items
) {
    public record ApprovedReturnItem(Long orderItemId, String itemCode, Integer quantity) {}
}
