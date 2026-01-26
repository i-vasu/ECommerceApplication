package com.app.product.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.product.entities.Product;
import com.app.product.entities.Category;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

	Page<Product> findByProductNameLike(String keyword, Pageable pageDetails);

	Product findByProductName(String productName);

	Product findByItemCode(String itemCode);

	List<Product> findByCategory(Category category);

	// ParadeDB BM25 Search Query
	// Syntax: WHERE index_name @@@ 'query_string'
	@Query(value = "SELECT * FROM products p WHERE products_search_idx @@@ :keyword", nativeQuery = true)
	Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

	@Query(value = "SELECT * FROM products p WHERE (:keyword IS NULL OR products_search_idx @@@ :keyword) " +
			"AND (:minPrice IS NULL OR p.price >= :minPrice) " +
			"AND (:maxPrice IS NULL OR p.price <= :maxPrice)", nativeQuery = true)
	Page<Product> facetedSearchByKeyword(@Param("keyword") String keyword,
			@Param("minPrice") java.math.BigDecimal minPrice,
			@Param("maxPrice") java.math.BigDecimal maxPrice,
			Pageable pageable);

}
