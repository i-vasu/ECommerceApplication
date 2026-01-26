package com.app.review.repositories;

import com.app.review.entities.ProductReview;
import com.app.product.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepo extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProduct(Product product);

    List<ProductReview> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.productId = :productId")
    Double getAverageRatingByProductId(Long productId);

    boolean existsByEmailAndProduct(String email, Product product);
}
