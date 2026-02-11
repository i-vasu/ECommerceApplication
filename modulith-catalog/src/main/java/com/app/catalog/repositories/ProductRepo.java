package com.app.catalog.repositories;

import com.app.catalog.entities.Category;
import com.app.catalog.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

	Page<Product> findByProductNameLike(String keyword, Pageable pageDetails);

	Product findByProductName(String productName);

	Product findByItemCode(String itemCode);

	Page<Product> findByCategory(Category category, Pageable pageable);
	List<Product> findByCategory(Category category);

	// ParadeDB BM25 Search Query
	// Syntax: WHERE index_name @@@ 'query_string'
	// ParadeDB BM25 Search Query with Quality Deprioritization
	@Query(value = "SELECT * FROM products p WHERE products_search_idx @@@ :keyword ORDER BY quality_score DESC", nativeQuery = true)
	Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

	@Query(value = "SELECT * FROM products p WHERE (:keyword IS NULL OR products_search_idx @@@ :keyword) " +
			"AND (:minPrice IS NULL OR p.price >= :minPrice) " +
			"AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
			"ORDER BY quality_score DESC", nativeQuery = true)
	Page<Product> facetedSearchByKeyword(@Param("keyword") String keyword,
			@Param("minPrice") java.math.BigDecimal minPrice,
			@Param("maxPrice") java.math.BigDecimal maxPrice,
			Pageable pageable);

}
