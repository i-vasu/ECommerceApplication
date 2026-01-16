package com.app.catalog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
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

import com.app.entites.Category;
import com.app.entites.Product;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.ProductDTO;
import com.app.payloads.ProductResponse;
import com.app.repositories.CategoryRepo;
import com.app.repositories.ProductRepo;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	private ProductRepo productRepo;

	@Autowired
	private CategoryRepo categoryRepo;

	@Autowired
	private ModelMapper modelMapper;

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

			return modelMapper.map(savedProduct, ProductDTO.class);
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
				.map(this::convertToDTO)
				.collect(Collectors.toList());

		ProductResponse productResponse = new ProductResponse();

		productResponse.setContent(productDTOs);
		productResponse.setPageNumber(pageProducts.getNumber());
		productResponse.setPageSize(pageProducts.getSize());
		productResponse.setTotalElements(pageProducts.getTotalElements());
		productResponse.setTotalPages(pageProducts.getTotalPages());
		productResponse.setLastPage(pageProducts.isLast());

		return productResponse;
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
				.map(this::convertToDTO)
				.collect(Collectors.toList());

		ProductResponse productResponse = new ProductResponse();

		productResponse.setContent(productDTOs);
		productResponse.setPageNumber(pageProducts.getNumber());
		productResponse.setPageSize(pageProducts.getSize());
		productResponse.setTotalElements(pageProducts.getTotalElements());
		productResponse.setTotalPages(pageProducts.getTotalPages());
		productResponse.setLastPage(pageProducts.isLast());

		return productResponse;
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
				.map(this::convertToDTO)
				.collect(Collectors.toList());

		ProductResponse productResponse = new ProductResponse();

		productResponse.setContent(productDTOs);
		productResponse.setPageNumber(pageProducts.getNumber());
		productResponse.setPageSize(pageProducts.getSize());
		productResponse.setTotalElements(pageProducts.getTotalElements());
		productResponse.setTotalPages(pageProducts.getTotalPages());
		productResponse.setLastPage(pageProducts.isLast());

		return productResponse;
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
			com.app.payloads.ProductEvent event = new com.app.payloads.ProductEvent(productId, "UPDATED");
			String eventJson = new ObjectMapper().writeValueAsString(event);
			redisTemplate.convertAndSend("product-sync-topic", eventJson);
		} catch (Exception e) {
			System.err.println("Failed to publish product update event to Redis: " + e.getMessage());
		}

		return convertToDTO(savedProduct);
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
	public ProductDTO addReview(Long productId, com.app.payloads.ProductReviewDTO reviewDTO) {
		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		com.app.entites.ProductReview review = modelMapper.map(reviewDTO, com.app.entites.ProductReview.class);
		review.setProduct(product);
		review.setCreatedAt(java.time.LocalDateTime.now());

		product.getReviews().add(review);

		Product updatedProduct = productRepo.save(product);
		return convertToDTO(updatedProduct);
	}

	private ProductDTO convertToDTO(Product product) {
		ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);

		// Media URLs are now handled directly as strings from ERPNext

		// Map Media URLs
		if (product.getMedia() != null) {
			java.util.List<com.app.payloads.ProductMediaDTO> mediaDTOs = product.getMedia().stream().map(m -> {
				return modelMapper.map(m, com.app.payloads.ProductMediaDTO.class);
			}).collect(Collectors.toList());
			productDTO.setMedia(mediaDTOs);
		}

		// Map Reviews
		if (product.getReviews() != null) {
			java.util.List<com.app.payloads.ProductReviewDTO> reviewDTOs = product.getReviews().stream()
					.map(r -> modelMapper.map(r, com.app.payloads.ProductReviewDTO.class))
					.collect(Collectors.toList());
			productDTO.setReviews(reviewDTOs);
		}

		return productDTO;
	}

	@Override
	public InputStream getProductImage(String fileName) throws FileNotFoundException {
		throw new UnsupportedOperationException("Image serving is handled by ERPNext");
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
			com.app.payloads.ProductEvent event = new com.app.payloads.ProductEvent(productId, "DELETED");
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
		return modelMapper.map(product, ProductDTO.class);
	}

	@Override
	public List<ProductDTO> getProductsByIds(List<Long> productIds) {
		List<Product> products = productRepo.findAllById(productIds);
		return products.stream()
				.map(product -> modelMapper.map(product, ProductDTO.class))
				.collect(Collectors.toList());
	}
}
