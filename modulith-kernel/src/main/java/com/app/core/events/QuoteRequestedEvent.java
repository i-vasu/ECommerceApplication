package com.app.core.events;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record QuoteRequestedEvent(
    Long requestId,
    String customerName,
    String customerEmail,
    List<QuoteItemDTO> items,
    LocalDateTime requestedAt
) {
    @Builder
    public record QuoteItemDTO(
        String productName,
        Double price,
        Integer quantity
    ) {
        public String getProductName() { return productName; }
        public Double getPrice() { return price; }
        public Integer getQuantity() { return quantity; }
    }
}
