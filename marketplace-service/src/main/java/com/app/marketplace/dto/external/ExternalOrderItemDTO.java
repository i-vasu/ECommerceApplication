package com.app.marketplace.dto.external;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExternalOrderItemDTO {
    // Matching com.app.payloads.OrderItemDTO in order-service
    private Long orderItemId;
    // We might need product info
    private Long productId;
    private Integer quantity;
    private Double discount;
    private Double orderedProductPrice;
}
