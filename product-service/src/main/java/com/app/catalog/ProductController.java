package com.app.catalog;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
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

import com.app.config.AppConstants;
import com.app.entites.Product;
import com.app.payloads.ProductDTO;
import com.app.payloads.ProductResponse;
// import com.app.services.ProductService; // Same package now

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-Commerce Application")
public class ProductController {

	@Autowired
	private ProductService productService;

	@Autowired
	private com.app.inventory.ERPNextProductSyncService syncService;

	@PostMapping("/admin/products/sync")
	public ResponseEntity<String> syncProducts() {
		syncService.syncItems();
		return new ResponseEntity<String>("Sync initiated", HttpStatus.OK);
	}

	@PostMapping("/admin/categories/{categoryId}/product")
	public ResponseEntity<ProductDTO> addProduct(@Valid @RequestBody Product product, @PathVariable Long categoryId) {

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
	private com.app.search.AnalyticsService analyticsService;

	@GetMapping("/public/products/{productId}")
	public ResponseEntity<ProductDTO> getProduct(@PathVariable Long productId) {
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
	public ResponseEntity<java.util.List<ProductDTO>> getTrendingProducts() {
		java.util.List<ProductDTO> trending = analyticsService.getTrendingProducts();
		return new ResponseEntity<>(trending, HttpStatus.OK);
	}

	@PostMapping("/public/products/batch")
	public ResponseEntity<java.util.List<ProductDTO>> getProductsBatch(@RequestBody java.util.List<Long> productIds) {
		java.util.List<ProductDTO> products = productService.getProductsByIds(productIds);
		return new ResponseEntity<>(products, HttpStatus.OK);
	}

	@GetMapping("/public/categories/{categoryId}/products")
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
	public ResponseEntity<ProductResponse> getProductsByKeyword(@PathVariable String keyword,
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
		InputStream resource = productService.getProductImage(imageName);
		StreamUtils.copy(resource, response.getOutputStream());
	}

	@PutMapping("/admin/products/{productId}")
	public ResponseEntity<ProductDTO> updateProduct(@RequestBody Product product,
			@PathVariable Long productId) {
		ProductDTO updatedProduct = productService.updateProduct(productId, product);

		return new ResponseEntity<ProductDTO>(updatedProduct, HttpStatus.OK);
	}

	@PutMapping("/admin/products/{productId}/image")
	public ResponseEntity<ProductDTO> updateProductImage(@PathVariable Long productId,
			@RequestParam("image") MultipartFile image) throws IOException {
		ProductDTO updatedProduct = productService.updateProductImage(productId, image);

		return new ResponseEntity<ProductDTO>(updatedProduct, HttpStatus.OK);
	}

	@PostMapping("/admin/products/{productId}/media")
	public ResponseEntity<ProductDTO> addProductMedia(
			@PathVariable Long productId,
			@RequestParam("file") MultipartFile file,
			@RequestParam("type") String type) throws IOException {

		ProductDTO updatedProduct = productService.addMedia(productId, file, type);
		return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
	}

	@PostMapping("/public/products/{productId}/reviews")
	public ResponseEntity<ProductDTO> addProductReview(@PathVariable Long productId,
			@RequestBody com.app.payloads.ProductReviewDTO reviewDTO) {
		ProductDTO updatedProduct = productService.addReview(productId, reviewDTO);
		return new ResponseEntity<>(updatedProduct, HttpStatus.CREATED);
	}

	@DeleteMapping("/admin/products/{productId}")
	public ResponseEntity<String> deleteProductByCategory(@PathVariable Long productId) {
		String status = productService.deleteProduct(productId);

		return new ResponseEntity<String>(status, HttpStatus.OK);
	}

}
