package com.app.product;

import java.util.List;
import java.nio.file.Path;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import com.app.product.entities.Product;
import com.app.product.payloads.ProductDTO;
import com.app.product.payloads.ProductResponse;
import com.app.review.payloads.ProductReviewDTO;

public interface ProductService {

	ProductDTO addProduct(Long categoryId, Product product);

	ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

	ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy,
			String sortOrder);

	ProductDTO updateProduct(Long productId, Product product);

	ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException;

	ProductDTO addMedia(Long productId, MultipartFile file, String type) throws IOException;

	ProductDTO addReview(Long productId, ProductReviewDTO reviewDTO);

	Path getProductImagePath(String fileName) throws FileNotFoundException;

	ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy,
			String sortOrder);

	String deleteProduct(Long productId);

	ProductDTO getProductById(Long productId);

	List<ProductDTO> getProductsByIds(List<Long> productIds);

	List<String> getAllCategories();

	List<ProductDTO> getProductsByCategory(String category);
}
