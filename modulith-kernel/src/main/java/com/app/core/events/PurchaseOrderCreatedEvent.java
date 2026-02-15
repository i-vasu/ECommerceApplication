package com.app.core.events;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PurchaseOrderCreatedEvent(
    Long poId,
    String supplierName,
    List<POItemDTO> items,
    LocalDateTime createdAt,
    LocalDate expectedDeliveryDate
) {
    @Builder
    public record POItemDTO(
        String itemCode,
        Double unitPrice,
        Integer quantity
    ) {}
}
