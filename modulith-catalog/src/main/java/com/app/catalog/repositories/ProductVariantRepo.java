package com.app.catalog.repositories;

import com.app.catalog.entities.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepo extends JpaRepository<ProductVariant, Long> {

    ProductVariant findByItemCode(String itemCode);
}
