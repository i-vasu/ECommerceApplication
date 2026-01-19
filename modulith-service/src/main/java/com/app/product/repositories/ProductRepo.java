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
	
	@org.springframework.data.jpa.repository.Query(value = "SELECT * FROM products p WHERE to_tsvector('english', p.product_name || ' ' || p.description) @@ plainto_tsquery('english', :keyword)", nativeQuery = true)
	Page<Product> searchByKeyword(@org.springframework.data.repository.query.Param("keyword") String keyword, Pageable pageable);

}
