package com.app.product;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import com.app.product.entites.Product;
import com.app.product.payloads.ProductDTO;
import com.app.product.payloads.ProductResponse;

public interface ProductService {

	ProductDTO addProduct(Long categoryId, Product product);

	ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

	ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy,
			String sortOrder);

	ProductDTO updateProduct(Long productId, Product product);

	ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException;

	ProductDTO addMedia(Long productId, MultipartFile file, String type) throws IOException;

	ProductDTO addReview(Long productId, com.app.review.payloads.ProductReviewDTO reviewDTO);

	java.nio.file.Path getProductImagePath(String fileName) throws FileNotFoundException;

	ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy,
			String sortOrder);

	String deleteProduct(Long productId);

	ProductDTO getProductById(Long productId);

	java.util.List<ProductDTO> getProductsByIds(java.util.List<Long> productIds);

	java.util.List<String> getAllCategories();

	java.util.List<ProductDTO> getProductsByCategory(String category);
}
