package com.app.product.services;

import com.app.catalog.entities.Category;
import com.app.catalog.entities.Product;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.repositories.CategoryRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.services.ProductServiceImpl;
import com.app.core.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepo productRepo;
    @Mock
    private CategoryRepo categoryRepo;
    @Mock
    private com.app.catalog.mappers.ProductMapper productMapper;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    @Mock
    private com.app.core.async.EventProducer eventProducer;
    @Mock
    private com.app.catalog.review.services.SocialProofService socialProofService;
    @Mock
    private com.app.catalog.review.services.ReviewService reviewService;
    @Mock
    private com.app.core.services.PurchaseVerificationService purchaseVerificationService;
    @Mock
    private com.app.core.utils.ContentSanitizer sanitizer;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductDTO testProductDTO;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setCategoryId(1L);
        testCategory.setCategoryName("Apparel");
        testCategory.setProducts(new java.util.ArrayList<>()); // Initialize products list

        testProduct = new Product();
        testProduct.setProductId(1L);
        testProduct.setProductName("Silk Saree");
        testProduct.setPrice(BigDecimal.valueOf(5000.0));
        testProduct.setDiscount(BigDecimal.ZERO); // Initialize discount
        testProduct.setQuantity(10);
        testProduct.setCategory(testCategory);

        testProductDTO = new ProductDTO(1L, "Silk Saree", "ITEM001", "silk-saree.jpg", "Silk", 10,
                BigDecimal.valueOf(5000.0), BigDecimal.ZERO, BigDecimal.valueOf(5000.0), null, null, null, null);
    }

    @Test
    void testAddProduct_Success() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepo.save(any(Product.class))).thenReturn(testProduct);
        when(productMapper.productToProductDTO(testProduct)).thenReturn(testProductDTO);

        ProductDTO saved = productService.addProduct(1L, testProductDTO);

        assertNotNull(saved);
        assertEquals("Silk Saree", saved.productName());
        verify(productRepo).save(any());
    }

    @Test
    void testGetProductById_Found() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productMapper.productToProductDTO(testProduct)).thenReturn(testProductDTO);

        ProductDTO found = productService.getProductById(1L);

        assertNotNull(found);
        assertEquals(1L, found.productId());
    }

    @Test
    void testGetProductById_NotFound() {
        when(productRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void testUpdateProduct_Success() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepo.save(any(Product.class))).thenReturn(testProduct);
        when(productMapper.productToProductDTO(any(Product.class))).thenReturn(testProductDTO);

        ProductDTO updated = productService.updateProduct(1L, testProductDTO);

        assertNotNull(updated);
        verify(productRepo).save(any());
        // Verify EventProducer was called for Redis event
        verify(eventProducer).publish(eq("product_events"), any());
    }

    @Test
    void testDeleteProduct() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(testProduct));
        
        productService.deleteProduct(1L);
        
        verify(productRepo).delete(testProduct);
        // Verify EventProducer was called for Redis event
        verify(eventProducer).publish(eq("product_events"), any());
    }
}
