package com.app.analytics.services;

import com.app.product.payloads.ProductDTO;
import java.util.List;

public interface AnalyticsService {
    void trackProductView(Long productId);

    List<ProductDTO> getTrendingProducts();
}
