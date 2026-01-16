package com.app.services;

import com.app.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "search:";
    private static final int CACHE_TTL_MINUTES = 5;

    @Override
    public void indexProduct(ProductDTO product) {
        // No-op - ParadeDB auto-indexes on INSERT/UPDATE
        log.debug("Product auto-indexed by ParadeDB: {}", product.getId());
    }

    @Override
    public void indexProducts(List<ProductDTO> products) {
        // No-op - ParadeDB auto-indexes on INSERT/UPDATE
        log.debug("Products auto-indexed by ParadeDB: {} products", products.size());
    }

    @Override
    @Cacheable(value = "product-search", key = "#query")
    public List<ProductDTO> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // Check cache first
        String cacheKey = CACHE_PREFIX + query.toLowerCase();
        @SuppressWarnings("unchecked")
        List<ProductDTO> cached = (List<ProductDTO>) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Cache HIT for search query: {}", query);
            return cached;
        }

        log.info("Cache MISS for search query: {}, querying ParadeDB", query);

        // Build ParadeDB query
        String paradeQuery = String.format(
                "name:%s^2 OR description:%s OR category:%s",
                query, query, query);

        // Query ParadeDB with BM25 ranking
        String sql = """
                SELECT id, name, description, category, price, stock_quantity,
                       image_url, created_at, updated_at
                FROM products
                WHERE id @@@ paradedb.parse(?)
                ORDER BY paradedb.rank_bm25(id) DESC
                LIMIT 50
                """;

        try {
            List<ProductDTO> results = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper<>(ProductDTO.class),
                    paradeQuery);

            // Cache results
            if (!results.isEmpty()) {
                redisTemplate.opsForValue().set(
                        cacheKey,
                        results,
                        CACHE_TTL_MINUTES,
                        TimeUnit.MINUTES);
            }

            log.info("Found {} products for query: {}", results.size(), query);
            return results;

        } catch (Exception e) {
            log.error("Error searching products with ParadeDB: {}", e.getMessage(), e);

            // Fallback to simple ILIKE search
            return fallbackSearch(query);
        }
    }

    /**
     * Fallback search using simple ILIKE if ParadeDB fails
     */
    private List<ProductDTO> fallbackSearch(String query) {
        log.warn("Using fallback ILIKE search for query: {}", query);

        String sql = """
                SELECT id, name, description, category, price, stock_quantity,
                       image_url, created_at, updated_at
                FROM products
                WHERE name ILIKE ? OR description ILIKE ? OR category ILIKE ?
                ORDER BY
                    CASE
                        WHEN name ILIKE ? THEN 1
                        WHEN description ILIKE ? THEN 2
                        ELSE 3
                    END
                LIMIT 50
                """;

        String searchPattern = "%" + query + "%";
        return jdbcTemplate.query(
                sql,
                new BeanPropertyRowMapper<>(ProductDTO.class),
                searchPattern, searchPattern, searchPattern,
                searchPattern, searchPattern);
    }

    @Override
    public void deleteProduct(String productId) {
        // No-op - ParadeDB auto-removes on DELETE
        log.debug("Product auto-removed from search index: {}", productId);

        // Clear related cache entries
        redisTemplate.keys(CACHE_PREFIX + "*").forEach(key -> redisTemplate.delete(key));
    }

    /**
     * Fuzzy search with typo tolerance
     */
    public List<ProductDTO> fuzzySearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        log.info("Fuzzy search for query: {}", query);

        String sql = """
                SELECT id, name, description, category, price, stock_quantity,
                       image_url, created_at, updated_at
                FROM products
                WHERE id @@@ paradedb.fuzzy('name', ?, 2)
                   OR id @@@ paradedb.fuzzy('description', ?, 2)
                ORDER BY paradedb.rank_bm25(id) DESC
                LIMIT 50
                """;

        try {
            return jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper<>(ProductDTO.class),
                    query, query);
        } catch (Exception e) {
            log.error("Error in fuzzy search: {}", e.getMessage());
            return fallbackSearch(query);
        }
    }
}
