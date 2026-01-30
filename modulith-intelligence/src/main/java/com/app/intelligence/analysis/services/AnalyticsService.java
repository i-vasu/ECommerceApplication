package com.app.intelligence.analysis.services;

import com.app.catalog.payloads.ProductDTO;
import java.util.List;
import java.math.BigDecimal;

public interface AnalyticsService {
    void trackProductView(Long productId);

    void trackSearch(String keyword, int resultCount);

    void trackAddToCart(Long productId, String itemCode, BigDecimal price);

    List<ProductDTO> getTrendingProducts();
}
