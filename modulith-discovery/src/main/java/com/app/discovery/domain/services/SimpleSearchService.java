package com.app.discovery.domain.services;

import com.app.catalog.payloads.ProductDTO;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class SimpleSearchService implements SearchService {

    @Override
    public void indexProduct(ProductDTO product) {
        // Simple implementation for now
    }

    @Override
    public void indexProducts(List<ProductDTO> products) {
        // Simple implementation for now
    }

    @Override
    public List<ProductDTO> searchProducts(String query, Double minPrice, Double maxPrice) {
        return new ArrayList<>();
    }

    @Override
    public void deleteProduct(String productId) {
        // Simple implementation for now
    }
}
