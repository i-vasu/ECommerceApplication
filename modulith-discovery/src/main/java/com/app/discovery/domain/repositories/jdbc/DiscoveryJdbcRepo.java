package com.app.discovery.domain.repositories.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Map;

public interface DiscoveryJdbcRepo extends Repository<Object, Long> {

    @Query("SELECT product_id, product_name, item_code, price, image, description " +
            "FROM products " +
            "ORDER BY feature_vector <-> :vector::vector LIMIT :limit")
    List<Map<String, Object>> findSimilarProducts(@Param("vector") String vector, @Param("limit") int limit);

    @Query("SELECT p.*, " +
            "  paradedb.score(bm25.search('products_search_idx', :query)) as bm25_score, " +
            "  (1 - (p.embedding <=> paradedb.embed(:query)::vector)) as semantic_score " +
            "FROM products p " +
            "WHERE (p.description @@@ :query OR p.product_name @@@ :query) " +
            "  AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "  AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "ORDER BY (bm25_score * 0.7 + semantic_score * 0.3) DESC LIMIT 50")
    List<Map<String, Object>> searchHybrid(@Param("query") String query,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice);

    @Modifying
    @Query("UPDATE products SET feature_vector = :vector::vector WHERE product_id = :productId")
    void updateFeatureVector(@Param("productId") Long productId, @Param("vector") String vector);
}
