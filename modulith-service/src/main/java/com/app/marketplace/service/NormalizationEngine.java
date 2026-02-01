package com.app.marketplace.service;

import com.app.catalog.payloads.ProductDTO;
import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderItemDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class NormalizationEngine {

    public OrderDTO mapToInternal(String source, Map<String, Object> rawData) {
        if ("AMAZON".equalsIgnoreCase(source)) {
            return mapAmazon(rawData);
        } else if ("FLIPKART".equalsIgnoreCase(source)) {
            return mapFlipkart(rawData);
        }
        return OrderDTO.builder().source(source).build();
    }

    private OrderDTO mapAmazon(Map<String, Object> data) {
        String orderId = (String) data.get("AmazonOrderId");
        String email = null;
        if (data.get("BuyerInfo") instanceof Map<?, ?> buyerInfo) {
            email = (String) buyerInfo.get("BuyerEmail");
        }
        
        Double amount = null;
        if (data.get("OrderTotal") instanceof Map<?, ?> total) {
            Object amt = total.get("Amount");
            if (amt instanceof Number n) {
                amount = n.doubleValue();
            }
        }

        List<OrderItemDTO> items = new ArrayList<>();
        if (data.get("OrderItems") instanceof List<?> rawItems) {
            for (Object obj : rawItems) {
                if (obj instanceof Map<?, ?> itemMap) {
                    ProductDTO product = new ProductDTO(null, null, (String) itemMap.get("SellerSKU"), null, null, null, 0.0, 0.0, 0.0, null, null, null, null);
                    items.add(new OrderItemDTO(null, product, ((Number) itemMap.get("QuantityOrdered")).intValue(), null, java.math.BigDecimal.valueOf(((Number) ((Map<?, ?>) itemMap.get("ItemPrice")).get("Amount")).doubleValue())));
                }
            }
        }

        return OrderDTO.builder()
                .source("AMAZON")
                .marketplaceOrderId(orderId)
                .email(email)
                .totalAmount(amount != null ? java.math.BigDecimal.valueOf(amount) : null)
                .orderItems(items)
                .build();
    }

    private OrderDTO mapFlipkart(Map<String, Object> data) {
        String orderId = (String) data.get("orderId");
        String email = null;
        if (data.get("buyerDetails") instanceof Map<?, ?> buyer) {
            email = (String) buyer.get("email");
        }
        
        Double amount = null;
        Object amtObj = data.get("totalAmount");
        if (amtObj instanceof Number n) {
            amount = n.doubleValue();
        }

        List<OrderItemDTO> items = new ArrayList<>();
        if (data.get("orderItems") instanceof List<?> rawItems) {
            for (Object obj : rawItems) {
                if (obj instanceof Map<?, ?> itemMap) {
                    ProductDTO product = new ProductDTO(null, null, (String) itemMap.get("sku"), null, null, null, 0.0, 0.0, 0.0, null, null, null, null);
                    items.add(new OrderItemDTO(null, product, ((Number) itemMap.get("quantity")).intValue(), null, java.math.BigDecimal.valueOf(((Number) itemMap.get("price")).doubleValue())));
                }
            }
        }

        return OrderDTO.builder()
                .source("FLIPKART")
                .marketplaceOrderId(orderId)
                .email(email)
                .totalAmount(amount != null ? java.math.BigDecimal.valueOf(amount) : null)
                .orderItems(items)
                .build();
    }
}
