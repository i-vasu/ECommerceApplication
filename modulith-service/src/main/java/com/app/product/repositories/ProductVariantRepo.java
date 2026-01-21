package com.app.product.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.product.entities.ProductVariant;

@Repository
public interface ProductVariantRepo extends JpaRepository<ProductVariant, Long> {

    ProductVariant findByItemCode(String itemCode);
}
