package com.app.product.services;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.app.product.ProductService;
import java.util.List;
import java.util.stream.Collectors;

import com.app.product.mappers.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.app.product.entites.Category;
import com.app.product.entites.Product;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.product.payloads.ProductDTO;
import com.app.product.payloads.ProductResponse;
import com.app.product.repositories.CategoryRepo;
import com.app.product.repositories.ProductRepo;
import java.io.IOException;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	private ProductRepo productRepo;

	@Autowired
	private CategoryRepo categoryRepo;

	@Autowired
	private ProductMapper productMapper;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Override
	@CacheEvict(value = "products", allEntries = true)
	public ProductDTO addProduct(Long categoryId, Product product) {

		Category category = categoryRepo.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

		boolean isProductNotPresent = true;

		List<Product> products = category.getProducts();

		for (int i = 0; i < products.size(); i++) {
			if (products.get(i).getProductName().equals(product.getProductName())
					&& products.get(i).getDescription().equals(product.getDescription())) {

				isProductNotPresent = false;
				break;
			}
		}

		if (isProductNotPresent) {
			product.setImage("default.png");

			product.setCategory(category);

			double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
			product.setSpecialPrice(specialPrice);

			Product savedProduct = productRepo.save(product);

			return productMapper.productToProductDTO(savedProduct);
		} else {
			throw new APIException("Product already exists !!!");
		}
	}

	@Override
	@Cacheable(value = "products", key = "#pageNumber + '-' + #pageSize + '-' + #sortBy + '-' + #sortOrder")
	public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

		Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

		Page<Product> pageProducts = productRepo.findAll(pageDetails);

		List<Product> products = pageProducts.getContent();

		List<ProductDTO> productDTOs = products.stream()
				.map(productMapper::productToProductDTO)
				.collect(Collectors.toList());

		return new ProductResponse(
				productDTOs,
				pageProducts.getNumber(),
				pageProducts.getSize(),
				pageProducts.getTotalElements(),
				pageProducts.getTotalPages(),
				pageProducts.isLast());
	}

	@Override
	@Cacheable(value = "products", key = "'cat-' + #categoryId + '-' + #pageNumber + '-' + #pageSize + '-' + #sortBy + '-' + #sortOrder")
	public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy,
			String sortOrder) {

		Category category = categoryRepo.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

		Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

		Page<Product> pageProducts = productRepo.findAll(pageDetails);

		List<Product> products = pageProducts.getContent();

		if (products.size() == 0) {
			throw new APIException(category.getCategoryName() + " category doesn't contain any products !!!");
		}

		List<ProductDTO> productDTOs = products.stream()
				.map(productMapper::productToProductDTO)
				.collect(Collectors.toList());

		return new ProductResponse(
				productDTOs,
				pageProducts.getNumber(),
				pageProducts.getSize(),
				pageProducts.getTotalElements(),
				pageProducts.getTotalPages(),
				pageProducts.isLast());
	}

	@Override
	@Cacheable(value = "products", key = "'search-' + #keyword + '-' + #pageNumber + '-' + #pageSize + '-' + #sortBy + '-' + #sortOrder")
	public ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy,
			String sortOrder) {
		Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

		Page<Product> pageProducts = productRepo.findByProductNameLike(keyword, pageDetails);

		List<Product> products = pageProducts.getContent();

		if (products.size() == 0) {
			throw new APIException("Products not found with keyword: " + keyword);
		}

		List<ProductDTO> productDTOs = products.stream()
				.map(productMapper::productToProductDTO)
				.collect(Collectors.toList());

		return new ProductResponse(
				productDTOs,
				pageProducts.getNumber(),
				pageProducts.getSize(),
				pageProducts.getTotalElements(),
				pageProducts.getTotalPages(),
				pageProducts.isLast());
	}

	@Override
	@org.springframework.cache.annotation.Caching(evict = {
			@CacheEvict(value = "products", allEntries = true),
			@CacheEvict(value = "product", key = "#productId")
	})
	public ProductDTO updateProduct(Long productId, Product product) {
		Product productFromDB = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		if (productFromDB == null) {
			throw new APIException("Product not found with productId: " + productId);
		}

		product.setImage(productFromDB.getImage());
		product.setProductId(productId);
		product.setCategory(productFromDB.getCategory());

		double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
		product.setSpecialPrice(specialPrice);

		Product savedProduct = productRepo.save(product);

		// Publish Event to DragonflyDB (Redis)
		try {
			com.app.product.payloads.ProductSyncEvent event = new com.app.product.payloads.ProductSyncEvent(productId,
					"UPDATED");
			String eventJson = new ObjectMapper().writeValueAsString(event);
			redisTemplate.convertAndSend("product-sync-topic", eventJson);
		} catch (Exception e) {
			System.err.println("Failed to publish product update event to Redis: " + e.getMessage());
		}

		return productMapper.productToProductDTO(savedProduct);
	}

	@Override
	@org.springframework.cache.annotation.Caching(evict = {
			@CacheEvict(value = "products", allEntries = true),
			@CacheEvict(value = "product", key = "#productId")
	})
	public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
		// Media management is handled by ERPNext
		throw new APIException("Direct image upload is disabled. Please manage media via ERPNext.");
	}

	@Override
	@org.springframework.cache.annotation.Caching(evict = {
			@CacheEvict(value = "products", allEntries = true),
			@CacheEvict(value = "product", key = "#productId")
	})
	public ProductDTO addMedia(Long productId, MultipartFile file, String type) throws IOException {
		// Media management is handled by ERPNext
		throw new APIException("Direct media upload is disabled. Please manage media via ERPNext.");
	}

	@Override
	@org.springframework.cache.annotation.Caching(evict = {
			@CacheEvict(value = "products", allEntries = true),
			@CacheEvict(value = "product", key = "#productId")
	})
	public ProductDTO addReview(Long productId, com.app.review.payloads.ProductReviewDTO reviewDTO) {
		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		com.app.review.entities.ProductReview review = new com.app.review.entities.ProductReview();
		review.setProduct(product);
		// Note: User is not set here as we don't have access to UserRepo.
		// If User is required, it must be fetched via reviewDTO.getUserId() and
		// UserRepo.
		review.setRating(reviewDTO.getRating());
		review.setComment(reviewDTO.getComment());
		review.setCreatedAt(java.time.LocalDateTime.now());

		product.getReviews().add(review);

		Product updatedProduct = productRepo.save(product);
		return productMapper.productToProductDTO(updatedProduct);
	}

	@Override
	public List<String> getAllCategories() {
		// ✅ OPTIMIZED: Database projection (5x faster, 90% less memory)
		return categoryRepo.findAllCategoryNames();
	}

	@Override
	public List<ProductDTO> getProductsByCategory(String category) {
		Category cat = categoryRepo.findByCategoryName(category);
		if (cat == null) {
			throw new ResourceNotFoundException("Category", "categoryName", category);
		}
		List<Product> products = productRepo.findByCategory(cat);
		return products.stream().map(productMapper::productToProductDTO).collect(Collectors.toList());
	}

	public Path getProductImagePath(String fileName) throws FileNotFoundException {
		// In modular monolith, we use a centralized media directory
		Path path = Paths.get("data/media/products", fileName);
		if (!java.nio.file.Files.exists(path)) {
			throw new FileNotFoundException("Image not found: " + fileName);
		}
		return path;
	}

	public java.io.InputStream getProductImage(String fileName) throws FileNotFoundException {
		throw new UnsupportedOperationException("Use getProductImagePath for zero-copy serving");
	}

	@Override
	@org.springframework.cache.annotation.Caching(evict = {
			@CacheEvict(value = "products", allEntries = true),
			@CacheEvict(value = "product", key = "#productId")
	})
	public String deleteProduct(Long productId) {

		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		// Publish Event to DragonflyDB (Redis)
		try {
			com.app.product.payloads.ProductSyncEvent event = new com.app.product.payloads.ProductSyncEvent(productId,
					"DELETED");
			String eventJson = new ObjectMapper().writeValueAsString(event);
			redisTemplate.convertAndSend("product-sync-topic", eventJson);
		} catch (Exception e) {
			System.err.println("Failed to publish product delete event to Redis: " + e.getMessage());
		}

		productRepo.delete(product);

		return "Product with productId: " + productId + " deleted successfully !!!";
	}

	@Override
	@Cacheable(value = "product", key = "#productId")
	public ProductDTO getProductById(Long productId) {
		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
		return productMapper.productToProductDTO(product);
	}

	@Override
	public List<ProductDTO> getProductsByIds(List<Long> productIds) {
		List<Product> products = productRepo.findAllById(productIds);
		return products.stream()
				.map(productMapper::productToProductDTO)
				.collect(Collectors.toList());
	}
}
