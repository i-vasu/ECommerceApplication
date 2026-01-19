package com.app.search.repositories;

import com.app.product.entites.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ProductEmbeddingRepo {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveEmbedding(Long productId, float[] embedding) {
        String vectorStr = Arrays.toString(embedding); // "[0.1, 0.2, ...]"
        String sql = "INSERT INTO product_embeddings (product_id, embedding) VALUES (:pid, :emb::vector) " +
                "ON CONFLICT (product_id) DO UPDATE SET embedding = :emb::vector";

        entityManager.createNativeQuery(sql)
                .setParameter("pid", productId)
                .setParameter("emb", vectorStr)
                .executeUpdate();
    }

    public List<Long> searchSimilarProducts(float[] embedding, int limit) {
        String vectorStr = Arrays.toString(embedding);
        // Using L2 distance (<->) or Cosine distance (<=>). Cosine is usually better
        // for CLIP.
        String sql = "SELECT product_id FROM product_embeddings ORDER BY embedding <=> :emb::vector LIMIT :limit";

        List<Object> results = entityManager.createNativeQuery(sql)
                .setParameter("emb", vectorStr)
                .setParameter("limit", limit)
                .getResultList();

        return results.stream()
                .map(obj -> ((Number) obj).longValue())
                .collect(Collectors.toList());
    }
}
