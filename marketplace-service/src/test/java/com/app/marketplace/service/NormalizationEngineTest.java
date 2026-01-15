package com.app.marketplace.service;

import com.app.marketplace.dto.OrderDTO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NormalizationEngineTest {

    private final NormalizationEngine normalizationEngine = new NormalizationEngine();

    @Test
    void testMapAmazonOrder_Success() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("AmazonOrderId", "AMZN-123");
        rawData.put("BuyerEmail", "test@amazon.com");

        OrderDTO result = normalizationEngine.mapToInternal("AMAZON", rawData);

        assertEquals("AMZN-123", result.getMarketplaceOrderId());
        assertEquals("test@amazon.com", result.getCustomerEmail());
        assertEquals("AMAZON", result.getSource());
    }

    @Test
    void testMapFlipkartOrder_Success() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("orderId", "FLIP-456");

        OrderDTO result = normalizationEngine.mapToInternal("FLIPKART", rawData);

        assertEquals("FLIP-456", result.getMarketplaceOrderId());
        assertEquals("FLIPKART", result.getSource());
    }

    @Test
    void testMapUnknownSource() {
        Map<String, Object> rawData = new HashMap<>();

        OrderDTO result = normalizationEngine.mapToInternal("UNKNOWN", rawData);

        assertEquals("UNKNOWN", result.getSource());
        assertNull(result.getMarketplaceOrderId());
    }
}
