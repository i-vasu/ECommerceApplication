package com.app.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.entites.ProductReview;

@Repository
public interface ProductReviewRepo extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProduct_ProductId(Long productId);
}
