package com.app.core.events;

import java.time.LocalDateTime;
import java.util.List;

public record GoodsReceivedEvent(
    Long poId,
    Long vendorId,
    String supplierName,
    Double totalAmount,
    LocalDateTime receivedAt,
    List<ReceivedItem> items
) {
    public record ReceivedItem(String itemCode, Integer quantity, Double unitPrice) {}
}
