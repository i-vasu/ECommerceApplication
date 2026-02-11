package com.app.discovery.domain.services;

import com.app.catalog.payloads.ProductDTO;

import java.util.List;

public interface SearchService {
    void indexProduct(ProductDTO product);

    void indexProducts(List<ProductDTO> products);

    List<ProductDTO> searchProducts(String query, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    void deleteProduct(String productId);
}
