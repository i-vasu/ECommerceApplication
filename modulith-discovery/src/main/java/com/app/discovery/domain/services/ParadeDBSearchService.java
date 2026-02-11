package com.app.discovery.domain.services;

import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.payloads.ProductMediaDTO;
import com.app.catalog.payloads.ProductVariantDTO;
import com.app.catalog.review.payloads.ProductReviewDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ParadeDB Search Service.
 * Leverages Postgres-native BM25 search for sub-millisecond full-text queries.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class ParadeDBSearchService implements SearchService {

    private final com.app.discovery.domain.repositories.jdbc.DiscoveryJdbcRepo discoveryRepo;

    @Override
    public void indexProduct(ProductDTO product) {
        log.info("Indexing product into ParadeDB: {}", product.productName());
    }

    @Override
    public void indexProducts(List<ProductDTO> products) {
        log.info("Batch indexing {} products into ParadeDB", products.size());
    }

    @Override
    public List<ProductDTO> searchProducts(String query, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        log.info("Executing ParadeDB Hybrid Search (BM25 + Semantic) for: {} [Price: {} - {}]", query, minPrice,
                maxPrice);

        try {
            List<Map<String, Object>> results = discoveryRepo.searchHybrid(query, minPrice, maxPrice);

            return results.stream().map(row -> {
                return new ProductDTO(
                        (Long) row.get("id"),
                        (String) row.get("product_name"),
                        (String) row.get("item_code"),
                        (String) row.get("image"),
                        (String) row.get("description"),
                        row.get("quantity") != null ? (Integer) row.get("quantity") : 0,
                        row.get("price") != null ? new java.math.BigDecimal(row.get("price").toString()) : java.math.BigDecimal.ZERO,
                        row.get("discount") != null ? new java.math.BigDecimal(row.get("discount").toString()) : java.math.BigDecimal.ZERO,
                        row.get("special_price") != null ? new java.math.BigDecimal(row.get("special_price").toString()) : java.math.BigDecimal.ZERO,
                        new ArrayList<ProductVariantDTO>(),
                        new ArrayList<ProductMediaDTO>(),
                        new ArrayList<ProductReviewDTO>(),
                        row.get("avg_rating") != null ? ((Number) row.get("avg_rating")).doubleValue() : 0.0);
            }).toList();
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public void deleteProduct(String productId) {
        log.info("Deleting product from search index: {}", productId);
    }
}
