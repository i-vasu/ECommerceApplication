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

import com.app.product.config.AppConstants;
import com.app.product.entities.Product;
import com.app.product.payloads.ProductDTO;
import com.app.product.payloads.ProductResponse;
import com.app.product.ProductService;
import com.app.core.multitenancy.TenantRepository;
import com.app.admin.services.ERPNextProductSyncService;
import com.app.admin.services.ERPNextSetupService;
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
@RequestMapping("/api/v1")
@SecurityRequirement(name = "E-Commerce Application")
public class ProductController implements ProductApi {

	@Autowired
	private ProductService productService;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private ERPNextProductSyncService syncService;

	@Autowired
	private ERPNextSetupService setupService;

	@PostMapping("/admin/setup/erpnext")
	public ResponseEntity<String> setupERPNext() {
		return new ResponseEntity<>(setupService.setup(), HttpStatus.OK);
	}

	@PostMapping("/admin/products/sync")
	public ResponseEntity<String> syncProducts() {
		String tenantId = TenantContext.getTenantId();
		Tenant tenant = tenantRepository.findByTenantId(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

		syncService.syncItems(tenant);
		return new ResponseEntity<String>("Sync initiated for tenant: " + tenantId, HttpStatus.OK);
	}

	@PostMapping("/admin/categories/{categoryId}/product")
	public ResponseEntity<ProductDTO> addProduct(@Valid @RequestBody @NonNull Product product,
			@PathVariable @NonNull Long categoryId) {

		ProductDTO savedProduct = productService.addProduct(categoryId, product);

		return new ResponseEntity<ProductDTO>(savedProduct, HttpStatus.CREATED);
	}

	@GetMapping("/public/products")
	public ResponseEntity<ProductResponse> getAllProducts(
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		ProductResponse productResponse = productService.getAllProducts(pageNumber, pageSize, sortBy, sortOrder);

		return new ResponseEntity<ProductResponse>(productResponse, HttpStatus.OK);
	}

	@Autowired
	private AnalyticsService analyticsService;

	@GetMapping("/public/products/{productId}")
	@Override
	public ResponseEntity<ProductDTO> getProductById(@PathVariable @NonNull Long productId) {
		// Async tracking so we don't block the user
		try {
			// In production, use @Async or a separate thread/event
			analyticsService.trackProductView(productId);
		} catch (Exception e) {
			// Ignore tracking errors
		}

		ProductDTO productDTO = productService.getProductById(productId);
		return new ResponseEntity<ProductDTO>(productDTO, HttpStatus.OK);
	}

	@GetMapping("/public/products/trending")
	public ResponseEntity<List<ProductDTO>> getTrendingProducts() {
		List<ProductDTO> trending = analyticsService.getTrendingProducts();
		return new ResponseEntity<>(trending, HttpStatus.OK);
	}

	@PostMapping("/public/products/batch")
	@Override
	public ResponseEntity<List<ProductDTO>> getProductsByIds(@RequestBody List<Long> productIds) {
		List<ProductDTO> products = productService.getProductsByIds(productIds);
		return new ResponseEntity<>(products, HttpStatus.OK);
	}

	@GetMapping("/public/categories/{categoryId}/products")
	@Override
	public ResponseEntity<ProductResponse> getProductsByCategory(@PathVariable Long categoryId,
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		ProductResponse productResponse = productService.searchByCategory(categoryId, pageNumber, pageSize, sortBy,
				sortOrder);

		return new ResponseEntity<ProductResponse>(productResponse, HttpStatus.OK);
	}

	@GetMapping("/public/products/keyword/{keyword}")
	@Override
	public ResponseEntity<ProductResponse> searchProductByKeyword(@PathVariable String keyword,
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		ProductResponse productResponse = productService.searchProductByKeyword(keyword, pageNumber, pageSize, sortBy,
				sortOrder);

		return new ResponseEntity<ProductResponse>(productResponse, HttpStatus.OK);
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
	public ResponseEntity<ProductDTO> updateProduct(@RequestBody Product product,
			@PathVariable Long productId) {
		ProductDTO updatedProduct = productService.updateProduct(productId, product);

		return new ResponseEntity<ProductDTO>(updatedProduct, HttpStatus.OK);
	}

	@PutMapping("/admin/products/{productId}/image")
	@Override
	public ResponseEntity<ProductDTO> updateProductImage(@PathVariable Long productId,
			@RequestParam("image") MultipartFile image) throws IOException {
		ProductDTO updatedProduct = productService.updateProductImage(productId, image);

		return new ResponseEntity<ProductDTO>(updatedProduct, HttpStatus.OK);
	}

	@PostMapping("/admin/products/{productId}/media")
	@Override
	public ResponseEntity<ProductDTO> addMedia(
			@PathVariable Long productId,
			@RequestParam("file") MultipartFile file,
			@RequestParam("type") String type) throws IOException {

		ProductDTO updatedProduct = productService.addMedia(productId, file, type);
		return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
	}

	@PostMapping("/public/products/{productId}/reviews")
	@Override
	public ResponseEntity<ProductDTO> addReview(@PathVariable Long productId,
			@RequestBody ProductReviewDTO reviewDTO) {
		ProductDTO updatedProduct = productService.addReview(productId, reviewDTO);
		return new ResponseEntity<>(updatedProduct, HttpStatus.CREATED);
	}

	@DeleteMapping("/admin/products/{productId}")
	@Override
	public ResponseEntity<String> deleteProduct(@PathVariable Long productId) {
		String status = productService.deleteProduct(productId);

		return new ResponseEntity<String>(status, HttpStatus.OK);
	}

}
