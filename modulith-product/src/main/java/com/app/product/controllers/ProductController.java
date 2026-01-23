package com.app.product.controllers;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import com.app.config.AppConstants;
import com.app.product.entities.Product;
import com.app.product.payloads.ProductDTO;
import com.app.product.payloads.ProductResponse;
import com.app.product.ProductService;
import com.app.core.multitenancy.TenantRepository;
import com.app.core.multitenancy.TenantContext;
import com.app.core.multitenancy.Tenant;
import com.app.core.ResourceNotFoundException;
import com.app.analytics.services.AnalyticsService;
import com.app.review.payloads.ProductReviewDTO;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.channels.Channels;
import java.nio.file.StandardOpenOption;

@RestController
@RequestMapping("/api")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
public class ProductController implements ProductApi {

	@Autowired
	private ProductService productService;

	@Autowired
	private AnalyticsService analyticsService;

	@PostMapping("/admin/categories/{categoryId}/product")
	public ResponseEntity<ApiResponse<ProductDTO>> addProduct(@Valid @RequestBody @NonNull Product product,
			@PathVariable @NonNull Long categoryId) {

		ProductDTO savedProduct = productService.addProduct(categoryId, product);

		return new ResponseEntity<>(ApiResponse.success(savedProduct, "Product added successfully"),
				HttpStatus.CREATED);
	}

	@GetMapping("/public/products")
	public ResponseEntity<ApiResponse<ProductResponse>> getAllProducts(
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		ProductResponse productResponse = productService.getAllProducts(pageNumber, pageSize, sortBy, sortOrder);

		return ResponseEntity.ok(ApiResponse.success(productResponse, "Products retrieved successfully"));
	}

	@GetMapping("/public/products/{productId}")
	@Override
	public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable @NonNull Long productId) {
		// Async tracking so we don't block the user
		try {
			// In production, use @Async or a separate thread/event
			analyticsService.trackProductView(productId);
		} catch (Exception e) {
			// Ignore tracking errors
		}

		ProductDTO productDTO = productService.getProductById(productId);
		return ResponseEntity.ok(ApiResponse.success(productDTO, "Product retrieved successfully"));
	}

	@GetMapping("/public/products/trending")
	public ResponseEntity<ApiResponse<List<ProductDTO>>> getTrendingProducts() {
		List<ProductDTO> trending = analyticsService.getTrendingProducts();
		return ResponseEntity.ok(ApiResponse.success(trending, "Trending products retrieved successfully"));
	}

	@PostMapping("/public/products/batch")
	@Override
	public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsByIds(@RequestBody List<Long> productIds) {
		List<ProductDTO> products = productService.getProductsByIds(productIds);
		return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
	}

	@GetMapping("/public/categories/{categoryId}/products")
	@Override
	public ResponseEntity<ApiResponse<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId,
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		ProductResponse productResponse = productService.searchByCategory(categoryId, pageNumber, pageSize, sortBy,
				sortOrder);

		return ResponseEntity.ok(ApiResponse.success(productResponse, "Products retrieved successfully"));
	}

	@GetMapping("/public/products/keyword/{keyword}")
	@Override
	public ResponseEntity<ApiResponse<ProductResponse>> searchProductByKeyword(@PathVariable String keyword,
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		ProductResponse productResponse = productService.searchProductByKeyword(keyword, pageNumber, pageSize, sortBy,
				sortOrder);

		return ResponseEntity.ok(ApiResponse.success(productResponse, "Search results retrieved successfully"));
	}

	@GetMapping(value = "/public/products/image/{imageName}", produces = MediaType.IMAGE_JPEG_VALUE)
	public void serveImage(@PathVariable("imageName") String imageName, HttpServletResponse response)
			throws IOException {
		Path imagePath = productService.getProductImagePath(imageName);

		try (FileChannel fileChannel = FileChannel.open(imagePath,
				StandardOpenOption.READ)) {
			long size = fileChannel.size();
			response.setContentLengthLong(size);
			fileChannel.transferTo(0, size, Channels.newChannel(response.getOutputStream()));
		}
	}

	@PutMapping("/admin/products/{productId}")
	@Override
	public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(@RequestBody com.app.product.entities.Product product,
			@PathVariable Long productId) {
		ProductDTO updatedProduct = productService.updateProduct(productId, product);
		return ResponseEntity.ok(ApiResponse.success(updatedProduct, "Product updated successfully"));
	}

	@PutMapping("/admin/products/{productId}/image")
	@Override
	public ResponseEntity<ApiResponse<ProductDTO>> updateProductImage(@PathVariable Long productId,
			@RequestParam("image") MultipartFile image) throws IOException {
		ProductDTO updatedProduct = productService.updateProductImage(productId, image);
		return ResponseEntity.ok(ApiResponse.success(updatedProduct, "Image updated successfully"));
	}

	@PostMapping("/admin/products/{productId}/media")
	@Override
	public ResponseEntity<ApiResponse<ProductDTO>> addMedia(
			@PathVariable Long productId,
			@RequestParam("file") MultipartFile file,
			@RequestParam("type") String type) throws IOException {

		ProductDTO updatedProduct = productService.addMedia(productId, file, type);
		return ResponseEntity.ok(ApiResponse.success(updatedProduct, "Media added successfully"));
	}

	@PostMapping("/public/products/{productId}/reviews")
	@Override
	public ResponseEntity<ApiResponse<ProductDTO>> addReview(@PathVariable Long productId,
			@RequestBody ProductReviewDTO reviewDTO) {
		ProductDTO updatedProduct = productService.addReview(productId, reviewDTO);
		return new ResponseEntity<>(ApiResponse.success(updatedProduct, "Review added successfully"),
				HttpStatus.CREATED);
	}

	@DeleteMapping("/admin/products/{productId}")
	@Override
	public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long productId) {
		String status = productService.deleteProduct(productId);
		return ResponseEntity.ok(ApiResponse.success(status, "Product deleted successfully"));
	}

}
