package com.app.marketplace.service;

import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderItemDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NormalizationEngine {

    public OrderDTO mapToInternal(String source, Map<String, Object> rawData) {
        var builder = OrderDTO.builder();
        builder.source(source);

        // Basic mapping logic - this would need to be expanded based on actual schema
        if ("AMAZON".equalsIgnoreCase(source)) {
            mapAmazonOrder(builder, rawData);
        } else if ("FLIPKART".equalsIgnoreCase(source)) {
            mapFlipkartOrder(builder, rawData);
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private void mapAmazonOrder(OrderDTO.OrderDTOBuilder builder, Map<String, Object> data) {
        builder.marketplaceOrderId((String) data.getOrDefault("AmazonOrderId", "UNKNOWN"));

        Map<String, Object> buyerInfo = (Map<String, Object>) data.get("BuyerInfo");
        if (buyerInfo != null) {
            builder.customerEmail((String) buyerInfo.getOrDefault("BuyerEmail", "unknown@amazon.com"));
            builder.email((String) buyerInfo.getOrDefault("BuyerEmail", "unknown@amazon.com"));
        }

        Map<String, Object> orderTotal = (Map<String, Object>) data.get("OrderTotal");
        if (orderTotal != null) {
            Object amount = orderTotal.get("Amount");
            if (amount instanceof Number) {
                builder.totalAmount(((Number) amount).doubleValue());
            } else if (amount instanceof String) {
                builder.totalAmount(Double.parseDouble((String) amount));
            }
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("OrderItems");
        if (items != null) {
            List<OrderItemDTO> orderItems = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String sku = (String) item.getOrDefault("SellerSKU", "UNKNOWN");
                String title = (String) item.getOrDefault("Title", "Unknown Product");
                int qty = 1;
                Object qtyObj = item.get("QuantityOrdered");
                if (qtyObj instanceof Number)
                    qty = ((Number) qtyObj).intValue();

                double price = 0.0;
                Map<String, Object> itemPrice = (Map<String, Object>) item.get("ItemPrice");
                if (itemPrice != null) {
                    Object pObj = itemPrice.get("Amount");
                    if (pObj instanceof Number)
                        price = ((Number) pObj).doubleValue();
                }

                // Create partial ProductDTO
                com.app.product.payloads.ProductDTO product = new com.app.product.payloads.ProductDTO(
                        null, title, sku, null, null, null, price, 0.0, price, null, null, null, null,
                        new java.util.HashMap<>());

                orderItems.add(new OrderItemDTO(null, product, qty, 0.0, price));
            }
            builder.orderItems(orderItems);
        }
    }

    @SuppressWarnings("unchecked")
    private void mapFlipkartOrder(OrderDTO.OrderDTOBuilder builder, Map<String, Object> data) {
        builder.marketplaceOrderId((String) data.getOrDefault("orderId", "UNKNOWN"));

        Map<String, Object> buyerDetails = (Map<String, Object>) data.get("buyerDetails");
        if (buyerDetails != null) {
            builder.customerEmail((String) buyerDetails.getOrDefault("email", "unknown@flipkart.com"));
            builder.email((String) buyerDetails.getOrDefault("email", "unknown@flipkart.com"));
        }

        Object amount = data.get("totalAmount");
        if (amount instanceof Number) {
            builder.totalAmount(((Number) amount).doubleValue());
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("orderItems");
        if (items != null) {
            List<OrderItemDTO> orderItems = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String sku = (String) item.getOrDefault("sku", "UNKNOWN");
                String title = (String) item.getOrDefault("title", "Unknown Product");
                int qty = 1;
                Object qtyObj = item.get("quantity");
                if (qtyObj instanceof Number)
                    qty = ((Number) qtyObj).intValue();

                double price = 0.0;
                Object pObj = item.get("price");
                if (pObj instanceof Number)
                    price = ((Number) pObj).doubleValue();

                // Create partial ProductDTO
                com.app.product.payloads.ProductDTO product = new com.app.product.payloads.ProductDTO(
                        null, title, sku, null, null, null, price, 0.0, price, null, null, null, null,
                        new java.util.HashMap<>());

                orderItems.add(new OrderItemDTO(null, product, qty, 0.0, price));
            }
            builder.orderItems(orderItems);
        }
    }
}
