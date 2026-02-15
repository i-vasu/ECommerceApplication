package com.app.discovery.domain.services;

import com.app.catalog.mappers.ProductMapper;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.repositories.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Simple fallback search service using ParadeDB BM25 full-text search.
 * Used when semantic/vector search is unavailable.
 */
@Service
@RequiredArgsConstructor
public class SimpleSearchService implements SearchService {

    private final ProductRepo productRepo;
    private final ProductMapper productMapper;

    @Override
    public void indexProduct(ProductDTO product) {
        // ParadeDB auto-indexes via triggers, no manual indexing needed
    }

    @Override
    public void indexProducts(List<ProductDTO> products) {
        // ParadeDB auto-indexes via triggers, no manual indexing needed
    }

    @Override
    public List<ProductDTO> searchProducts(String query, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // Use ParadeDB BM25 search with faceted filtering
        var results = productRepo.facetedSearchByKeyword(
            query, 
            minPrice, 
            maxPrice, 
            PageRequest.of(0, 20)
        );

        return results.stream()
                .map(productMapper::productToProductDTO)
                .toList();
    }

    @Override
    public void deleteProduct(String productId) {
        // ParadeDB auto-removes from index when product is deleted
    }
}
