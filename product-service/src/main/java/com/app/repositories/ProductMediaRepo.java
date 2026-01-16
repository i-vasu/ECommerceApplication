package com.app.repositories;

import com.app.entites.Product;
import com.app.entites.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMediaRepo extends JpaRepository<ProductMedia, Long> {
    List<ProductMedia> findByProduct(Product product);

    void deleteByProduct(Product product);
}
