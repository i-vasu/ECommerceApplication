package com.app.product.services;

import com.app.product.payloads.ProductDTO;
import java.util.List;

public interface PersonalizationService {
    void trackProductView(Long userId, Long productId);

    List<ProductDTO> getRecentlyViewed(Long userId, int limit);

    List<ProductDTO> getNewlyDropped(int limit);

    List<ProductDTO> getTrending(int limit);

    List<ProductDTO> getBestSellers(int limit);

    void trackSearch(Long userId, String keyword);

    List<String> getSearchHistory(Long userId, int limit);
}
