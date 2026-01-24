package com.app.marketplace.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
}
