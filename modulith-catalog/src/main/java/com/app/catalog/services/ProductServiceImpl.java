package com.app.catalog.services;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import com.app.catalog.ProductService;
import com.app.core.audit.AuditTrail;
import com.app.core.utils.ContentSanitizer;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

import com.app.catalog.mappers.ProductMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.context.SecurityContextHolder;
import com.app.security.repositories.UserRepo;

import com.app.catalog.entities.Category;
import com.app.catalog.entities.Product;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.payloads.ProductResponse;
import com.app.catalog.payloads.ProductSyncEvent;
import com.app.catalog.repositories.CategoryRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.review.services.ReviewService;
import com.app.catalog.review.services.SocialProofService;
import com.app.catalog.review.entities.ProductReview;
import com.app.catalog.review.payloads.ProductReviewDTO;
import com.app.core.async.EventProducer;
import java.io.IOException;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@RequiredArgsConstructor
@Transactional
@Service
public class ProductServiceImpl implements ProductService {

	private static final Logger log = LogManager.getLogger(ProductServiceImpl.class);

	private final ProductRepo productRepo;
	private final CategoryRepo categoryRepo;
	private final ProductMapper productMapper;
	private final StringRedisTemplate redisTemplate;
	private final EventProducer eventProducer;
	private final UserRepo userRepo;
	private final ContentSanitizer sanitizer;
	private final SocialProofService socialProofService;
	private final ReviewService reviewService;
	private final org.springframework.context.ApplicationEventPublisher eventPublisher;

	@Override
	@CacheEvict(value = "products", allEntries = true)
	@AuditTrail(action = "ADD_PRODUCT")
	public ProductDTO addProduct(Long categoryId, Product product) {

		var category = categoryRepo.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

		var isProductNotPresent = true;
		var products = category.getProducts();

		for (var p : products) {
			if (p.getProductName().equals(product.getProductName())
					&& p.getDescription().equals(product.getDescription())) {
				isProductNotPresent = false;
				break;
			}
		}

		if (isProductNotPresent) {
			product.setImage("default.png");
			product.setCategory(category);

			// BigDecimal calculation: price * (1 - discount/100)
			java.math.BigDecimal discountFactor = java.math.BigDecimal.ONE.subtract(
					product.getDiscount().multiply(java.math.BigDecimal.valueOf(0.01)));
			java.math.BigDecimal specialPrice = product.getPrice().multiply(discountFactor)
					.setScale(2, java.math.RoundingMode.HALF_UP);

			product.setSpecialPrice(specialPrice);

			var savedProduct = productRepo.save(product);

			// DECOUPLED: Domain Event
			eventPublisher.publishEvent(new com.app.core.events.ProductCreatedEvent(
					savedProduct.getProductId(),
					savedProduct.getItemCode(),
					savedProduct.getProductName(),
					savedProduct.getSpecialPrice(),
					savedProduct.getQuantity(),
					savedProduct.getImage(),
					savedProduct.getTags()));

			return productMapper.productToProductDTO(savedProduct);
		} else {
			throw new APIException("Product already exists !!!");
		}
	}

	@Override
	@Cacheable(value = "products", key = "#pageNumber + '-' + #pageSize + '-' + #sortBy + '-' + #sortOrder")
	public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
		if (pageSize > com.app.config.AppConstants.MAX_PAGE_SIZE) {
			pageSize = com.app.config.AppConstants.MAX_PAGE_SIZE;
		}

		var sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		var pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
		var pageProducts = productRepo.findAll(pageDetails);
		var products = pageProducts.getContent();

		var productDTOs = products.stream()
				.map(productMapper::productToProductDTO)
				.toList();

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
		if (pageSize > com.app.config.AppConstants.MAX_PAGE_SIZE) {
			pageSize = com.app.config.AppConstants.MAX_PAGE_SIZE;
		}

		var category = categoryRepo.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

		var sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		var pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
		var pageProducts = productRepo.findAll(pageDetails);
		var products = pageProducts.getContent();

		if (products.isEmpty()) {
			throw new APIException(category.getCategoryName() + " category doesn't contain any products !!!");
		}

		var productDTOs = products.stream()
				.map(productMapper::productToProductDTO)
				.toList();

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
		if (keyword == null || keyword.trim().length() < com.app.config.AppConstants.MIN_KEYWORD_LENGTH) {
			throw new APIException("Search keyword must be at least " + com.app.config.AppConstants.MIN_KEYWORD_LENGTH
					+ " characters.");
		}

		if (pageSize > com.app.config.AppConstants.MAX_PAGE_SIZE) {
			pageSize = com.app.config.AppConstants.MAX_PAGE_SIZE;
		}

		var sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		var pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
		var pageProducts = productRepo.searchByKeyword(keyword, pageDetails);
		var products = pageProducts.getContent();

		if (products.isEmpty()) {
			eventPublisher.publishEvent(new com.app.core.events.ProductSearchEvent(keyword, null, 0));
			throw new APIException("Products not found with keyword: " + keyword);
		}

		eventPublisher.publishEvent(new com.app.core.events.ProductSearchEvent(keyword, null, products.size()));

		var productDTOs = products.stream()
				.map(productMapper::productToProductDTO)
				.toList();

		return new ProductResponse(
				productDTOs,
				pageProducts.getNumber(),
				pageProducts.getSize(),
				pageProducts.getTotalElements(),
				pageProducts.getTotalPages(),
				pageProducts.isLast());
	}

	@Override
	@Cacheable(value = "products", key = "'facet-' + #keyword + '-' + #minPrice + '-' + #maxPrice + '-' + #pageNumber + '-' + #pageSize + '-' + #sortBy + '-' + #sortOrder")
	public ProductResponse facetedSearch(String keyword, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice,
			Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

		if (pageSize > com.app.config.AppConstants.MAX_PAGE_SIZE) {
			pageSize = com.app.config.AppConstants.MAX_PAGE_SIZE;
		}

		var sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		var pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
		var pageProducts = productRepo.facetedSearchByKeyword(keyword, minPrice, maxPrice, pageDetails);
		var products = pageProducts.getContent();

		var productDTOs = products.stream()
				.map(productMapper::productToProductDTO)
				.toList();

		return new ProductResponse(
				productDTOs,
				pageProducts.getNumber(),
				pageProducts.getSize(),
				pageProducts.getTotalElements(),
				pageProducts.getTotalPages(),
				pageProducts.isLast());
	}

	@Override
	@Caching(evict = {
			@CacheEvict(value = "products", allEntries = true),
			@CacheEvict(value = "product", key = "#productId")
	})
	@AuditTrail(action = "UPDATE_PRODUCT")
	public ProductDTO updateProduct(Long productId, Product product) {
		var productFromDB = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		var oldPrice = productFromDB.getSpecialPrice();
		var oldQty = productFromDB.getQuantity();

		product.setImage(productFromDB.getImage());
		product.setProductId(productId);
		product.setCategory(productFromDB.getCategory());

		java.math.BigDecimal discountFactor = java.math.BigDecimal.ONE.subtract(
				product.getDiscount().multiply(java.math.BigDecimal.valueOf(0.01)));
		java.math.BigDecimal specialPrice = product.getPrice().multiply(discountFactor)
				.setScale(2, java.math.RoundingMode.HALF_UP);

		product.setSpecialPrice(specialPrice);

		var savedProduct = productRepo.save(product);

		// DECOUPLED: Domain Event
		eventPublisher.publishEvent(new com.app.core.events.ProductUpdatedEvent(
				productId,
				savedProduct.getItemCode(),
				oldPrice,
				savedProduct.getSpecialPrice(),
				oldQty,
				savedProduct.getQuantity(),
				savedProduct.getImage(),
				savedProduct.getTags()));

		try {
			var tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
			var event = new ProductSyncEvent(productId, "UPDATED", tenantId);
			eventProducer.publish("product_events", event);
		} catch (Exception e) {
			log.error("Failed to publish product update event to Redis: {}", e.getMessage());
		}

		return productMapper.productToProductDTO(savedProduct);
	}

	@Override
	public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
		throw new APIException("Direct image upload is disabled. Please manage media via ERPNext.");
	}

	@Override
	public ProductDTO addMedia(Long productId, MultipartFile file, String type) throws IOException {
		throw new APIException("Direct media upload is disabled. Please manage media via ERPNext.");
	}

	@Caching(evict = {
			@CacheEvict(value = "products", allEntries = true),
			@CacheEvict(value = "product", key = "#productId")
	})
	@Override
	public ProductDTO addReview(Long productId, ProductReviewDTO reviewDTO) {
		reviewService.addReview(productId, reviewDTO);
		return getProductById(productId);
	}

	@Override
	public List<String> getAllCategories() {
		return categoryRepo.findAllCategoryNames();
	}

	@Override
	public List<ProductDTO> getProductsByCategory(String category) {
		var cat = categoryRepo.findByCategoryName(category);
		if (cat == null) {
			throw new ResourceNotFoundException("Category", "categoryName", category);
		}
		var products = productRepo.findByCategory(cat);
		return products.stream()
				.map(productMapper::productToProductDTO)
				.toList();
	}

	@Override
	public Path getProductImagePath(String fileName) throws FileNotFoundException {
		var path = Paths.get("data/media/products", fileName);
		if (!Files.exists(path)) {
			throw new FileNotFoundException("Image not found: " + fileName);
		}
		return path;
	}

	@Override
	@Caching(evict = {
			@CacheEvict(value = "products", allEntries = true),
			@CacheEvict(value = "product", key = "#productId")
	})
	@AuditTrail(action = "DELETE_PRODUCT")
	public String deleteProduct(Long productId) {
		var product = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		String itemCode = product.getItemCode();

		try {
			var tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
			var event = new ProductSyncEvent(productId, "DELETED", tenantId);
			eventProducer.publish("product_events", event);
		} catch (Exception e) {
			log.error("Failed to publish product delete event to Redis: {}", e.getMessage());
		}

		productRepo.delete(product);

		// DECOUPLED: Domain Event
		eventPublisher.publishEvent(new com.app.core.events.ProductDeletedEvent(productId, itemCode));

		return "Product with productId: " + productId + " deleted successfully !!!";
	}

	@Override
	@Cacheable(value = "product", key = "#productId")
	public ProductDTO getProductById(Long productId) {
		var product = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		eventPublisher
				.publishEvent(new com.app.core.events.ProductViewedEvent(productId, null, product.getSpecialPrice()));
		socialProofService.incrementViewCount(productId);

		ProductDTO dto = productMapper.productToProductDTO(product);
		dto.socialPulse().putAll(socialProofService.getSocialPulse(productId));

		// Add Scarcity Signal
		String scarcity = null;
		if (product.getQuantity() > 0 && product.getQuantity() <= 5) {
			scarcity = "Hurry! Only " + product.getQuantity() + " left in stock.";
		} else if (product.getQuantity() == 0) {
			scarcity = "Currently Out of Stock";
		}

		return new ProductDTO(
				dto.productId(), dto.productName(), dto.itemCode(), dto.image(), dto.description(),
				dto.quantity(), dto.price(), dto.discount(), dto.specialPrice(),
				dto.variants(), dto.media(), dto.reviews(), dto.averageRating(),
				dto.socialPulse(), scarcity, null);
	}

	@Override
	public List<ProductDTO> getProductsByIds(List<Long> productIds) {
		var products = productRepo.findAllById(productIds);
		return products.stream()
				.map(productMapper::productToProductDTO)
				.toList();
	}

	/**
	 * Check if user has purchased this product.
	 * Uses Redis cache populated by PurchaseHistoryListener.
	 */
	private boolean checkUserPurchaseHistory(String email, Long productId) {
		try {
			String userKey = "user:purchases:" + email;
			Boolean isMember = redisTemplate.opsForSet().isMember(userKey, productId.toString());
			return Boolean.TRUE.equals(isMember);
		} catch (Exception e) {
			log.warn("Failed to check purchase history for user: {}, product: {}", email, productId, e);
			// On Redis failure, return false (conservative)
			return false;
		}
	}
}
