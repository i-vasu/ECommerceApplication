package com.app.marketplace.service;

import com.app.order.payloads.OrderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NormalizationEngineTest {

    private NormalizationEngine normalizationEngine;

    @BeforeEach
    void setUp() {
        normalizationEngine = new NormalizationEngine();
    }

    @Test
    void testMapAmazonOrder_FullData() {
        Map<String, Object> rawData = Map.of(
                "AmazonOrderId", "123-4567890-1234567",
                "BuyerInfo", Map.of("BuyerEmail", "buyer@example.com"),
                "OrderTotal", Map.of("Amount", 199.99),
                "OrderItems", List.of(
                        Map.of(
                                "SellerSKU", "SKU-AMZ-001",
                                "Title", "Amazon Product",
                                "QuantityOrdered", 2,
                                "ItemPrice", Map.of("Amount", 99.99))));

        OrderDTO orderDTO = normalizationEngine.mapToInternal("AMAZON", rawData);

        assertEquals("AMAZON", orderDTO.source());
        assertEquals("123-4567890-1234567", orderDTO.marketplaceOrderId());
        assertEquals("buyer@example.com", orderDTO.email());
        assertEquals(199.99, orderDTO.totalAmount().doubleValue());
        assertEquals(1, orderDTO.orderItems().size());
        assertEquals("SKU-AMZ-001", orderDTO.orderItems().get(0).product().itemCode());
        assertEquals(2, orderDTO.orderItems().get(0).quantity());
    }

    @Test
    void testMapFlipkartOrder_FullData() {
        Map<String, Object> rawData = Map.of(
                "orderId", "FLIP-98765",
                "buyerDetails", Map.of("email", "customer@flipkart.com"),
                "totalAmount", 250.0,
                "orderItems", List.of(
                        Map.of(
                                "sku", "SKU-FLP-002",
                                "title", "Flipkart Product",
                                "quantity", 1,
                                "price", 250.0)));

        OrderDTO orderDTO = normalizationEngine.mapToInternal("FLIPKART", rawData);

        assertEquals("FLIPKART", orderDTO.source());
        assertEquals("FLIP-98765", orderDTO.marketplaceOrderId());
        assertEquals("customer@flipkart.com", orderDTO.email());
        assertEquals(250.0, orderDTO.totalAmount().doubleValue());
        assertEquals(1, orderDTO.orderItems().size());
        assertEquals("SKU-FLP-002", orderDTO.orderItems().get(0).product().itemCode());
        assertEquals(1, orderDTO.orderItems().get(0).quantity());
    }

    @Test
    void testMapAmazonOrder_MissingFields() {
        Map<String, Object> rawData = Map.of(
                "AmazonOrderId", "555-666"
        // Missing BuyerInfo, OrderTotal, etc.
        );

        OrderDTO orderDTO = normalizationEngine.mapToInternal("AMAZON", rawData);

        assertEquals("AMAZON", orderDTO.source());
        assertEquals("555-666", orderDTO.marketplaceOrderId());
        assertNull(orderDTO.email()); // Note: builder default for email is null unless hit
        assertNull(orderDTO.totalAmount());
    }

    @Test
    void testMapFlipkartOrder_MissingFields() {
        Map<String, Object> rawData = Map.of(
                "orderId", "F-111");

        OrderDTO orderDTO = normalizationEngine.mapToInternal("FLIPKART", rawData);

        assertEquals("FLIPKART", orderDTO.source());
        assertEquals("F-111", orderDTO.marketplaceOrderId());
        assertNull(orderDTO.email());
    }
}
