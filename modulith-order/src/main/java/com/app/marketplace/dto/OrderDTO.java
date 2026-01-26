package com.app.marketplace.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDTO {
    private String marketplaceOrderId;
    private String customerEmail;
    private List<OrderItemDTO> items;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private String source; // e.g., AMAZON, FLIPKART
}
