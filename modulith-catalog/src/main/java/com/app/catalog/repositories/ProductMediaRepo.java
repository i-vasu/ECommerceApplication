package com.app.catalog.repositories;

import com.app.catalog.entities.Product;
import com.app.catalog.entities.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMediaRepo extends JpaRepository<ProductMedia, Long> {
    List<ProductMedia> findByProduct(Product product);

    void deleteByProduct(Product product);
}
