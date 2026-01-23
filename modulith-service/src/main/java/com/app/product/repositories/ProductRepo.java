package com.app.product.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.product.entites.Product;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

	Page<Product> findByProductNameLike(String keyword, Pageable pageDetails);

	Product findByProductName(String productName);

	Product findByItemCode(String itemCode);

	java.util.List<Product> findByCategory(com.app.product.entites.Category category);

	boolean existsByProductNameAndDescriptionAndCategory(String productName, String description, com.app.product.entites.Category category);

}
