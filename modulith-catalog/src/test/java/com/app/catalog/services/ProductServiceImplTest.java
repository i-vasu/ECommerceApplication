package com.app.catalog.services;

import com.app.catalog.entities.Product;
import com.app.catalog.mappers.ProductMapper;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.payloads.ProductSyncEvent;
import com.app.catalog.repositories.CategoryRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.review.services.ReviewService;
import com.app.catalog.review.services.SocialProofService;
import com.app.core.APIException;
import com.app.core.async.EventProducer;
import com.app.core.events.ProductUpdatedEvent;
import com.app.core.utils.ContentSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepo productRepo;
    @Mock
    private CategoryRepo categoryRepo;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private EventProducer eventProducer;
    @Mock
    private ContentSanitizer sanitizer;
    @Mock
    private SocialProofService socialProofService;
    @Mock
    private ReviewService reviewService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private Product productFromDb;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setProductId(1L);
        product.setProductName("Test Product");
        product.setPrice(BigDecimal.valueOf(100.0));
        product.setDiscount(BigDecimal.ZERO);
        product.setQuantity(10);
        product.setItemCode("TEST001");

        productFromDb = new Product();
        productFromDb.setProductId(1L);
        productFromDb.setProductName("Old Name");
        productFromDb.setPrice(BigDecimal.valueOf(90.0));
        productFromDb.setDiscount(BigDecimal.ZERO);
        productFromDb.setQuantity(5);
        productFromDb.setItemCode("TEST001");
        productFromDb.setSpecialPrice(BigDecimal.valueOf(90.0));
    }

    @Test
    void testUpdateProduct_Success() {
        // Arrange
        when(productRepo.findById(1L)).thenReturn(Optional.of(productFromDb));
        when(productRepo.save(any(Product.class))).thenReturn(productFromDb);
        when(productMapper.productToProductDTO(any())).thenReturn(new ProductDTO(1L, "Test Product", "TEST001", "image.png", "Desc", 10, BigDecimal.valueOf(100.0), BigDecimal.ZERO, BigDecimal.valueOf(100.0), null, null, null, 5.0));

        // Act
        productService.updateProduct(1L, product);

        // Assert
        verify(productRepo).save(productFromDb);
        assertEquals("Test Product", productFromDb.getProductName()); // Verified update happens on DB entity
        verify(eventPublisher).publishEvent(any(ProductUpdatedEvent.class));
        verify(eventProducer).publish(eq("product_events"), any(ProductSyncEvent.class));
    }

    @Test
    void testUpdateProduct_OptimisticLockingFailure() {
        // Arrange
        when(productRepo.findById(1L)).thenReturn(Optional.of(productFromDb));
        // Simulate concurrent update causing OptimisticLockingFailureException
        when(productRepo.save(any(Product.class))).thenThrow(new OptimisticLockingFailureException("Simulated concurrency error"));

        // Act & Assert
        Exception exception = assertThrows(APIException.class, () -> {
            productService.updateProduct(1L, product);
        });

        assertTrue(exception.getMessage().contains("updated by another user"));
        verify(productRepo).save(any(Product.class));
    }
    @Test
    void testAddProduct_Success() {
        // Arrange
        com.app.catalog.entities.Category category = new com.app.catalog.entities.Category();
        category.setCategoryId(1L);
        category.setCategoryName("Apparel");
        category.setProducts(new java.util.ArrayList<>());

        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
        when(productRepo.save(any(Product.class))).thenReturn(product);
        when(productMapper.productToProductDTO(product)).thenReturn(new ProductDTO(1L, "Test Product", "TEST001", "image.png", "Desc", 10, BigDecimal.valueOf(100.0), BigDecimal.ZERO, BigDecimal.valueOf(100.0), null, null, null, 0.0));

        // Act
        ProductDTO saved = productService.addProduct(1L, product);

        // Assert
        assertNotNull(saved);
        assertEquals("Test Product", saved.productName());
        verify(productRepo).save(any());
        verify(productRepo).save(any());
        // verify(eventProducer).publish(eq("product_events"), any(ProductSyncEvent.class));
        verify(eventPublisher).publishEvent(any(com.app.core.events.ProductCreatedEvent.class));
    }

    @Test
    void testGetProductById_Found() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(productFromDb));
        when(productMapper.productToProductDTO(productFromDb)).thenReturn(new ProductDTO(1L, "Old Name", "TEST001", "image.png", "Desc", 5, BigDecimal.valueOf(90.0), BigDecimal.ZERO, BigDecimal.valueOf(90.0), null, null, null, 0.0));

        ProductDTO found = productService.getProductById(1L);

        assertNotNull(found);
        assertEquals(1L, found.productId());
    }

    @Test
    void testGetProductById_NotFound() {
        when(productRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.app.core.ResourceNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void testDeleteProduct() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(productFromDb));
        
        productService.deleteProduct(1L);
        
        verify(productRepo).delete(productFromDb);
        verify(eventProducer).publish(eq("product_events"), any(ProductSyncEvent.class));
    }
}
