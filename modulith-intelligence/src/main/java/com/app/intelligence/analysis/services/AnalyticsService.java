package com.app.intelligence.analysis.services;

import com.app.catalog.payloads.ProductDTO;

import java.math.BigDecimal;
import java.util.List;

public interface AnalyticsService {
    void trackProductView(Long productId);

    void trackSearch(String keyword, int resultCount);

    void trackAddToCart(Long productId, String itemCode, BigDecimal price);

    List<ProductDTO> getTrendingProducts();
}
