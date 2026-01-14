package com.app.marketplace.dto.external;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ExternalOrderDTO {
    // Matching com.app.payloads.OrderDTO in order-service
    private Long orderId;
    private String email;
    // We'll use a simplified item structure or a dedicated one
    private List<ExternalOrderItemDTO> orderItems;
    private BigDecimal totalAmount;
    private String orderStatus;
}
