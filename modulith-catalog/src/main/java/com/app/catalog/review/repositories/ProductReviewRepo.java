package com.app.catalog.review.repositories;

import com.app.catalog.review.entities.ProductReview;
import com.app.catalog.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductReviewRepo extends JpaRepository<ProductReview, Long> {

    boolean existsByEmailAndProduct(String email, Product product);

    List<ProductReview> findByProduct(Product product);

    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.productId = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);
}
