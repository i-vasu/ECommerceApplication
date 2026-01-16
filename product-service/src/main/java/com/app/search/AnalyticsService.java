package com.app.search;

import com.app.payloads.ProductDTO;
import java.util.List;

public interface AnalyticsService {
    void trackProductView(Long productId);

    List<ProductDTO> getTrendingProducts();
}
