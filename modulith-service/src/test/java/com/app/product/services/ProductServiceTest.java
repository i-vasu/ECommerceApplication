package com.app.product.services;


import com.app.catalog.entities.Category;
import com.app.catalog.entities.Product;
import com.app.catalog.mappers.ProductMapper;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.repositories.CategoryRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.services.ProductServiceImpl;
import com.app.core.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private CategoryRepo categoryRepo;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private com.app.core.async.EventProducer eventProducer;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void addProduct_shouldSaveAndReturnDTO() {
        // Arrange
        Long categoryId = 10L;
        Product product = new Product();
        product.setProductName("Test Product");
        product.setDescription("Desc");
        product.setPrice(BigDecimal.valueOf(100.0));
        product.setDiscount(BigDecimal.valueOf(10.0));

        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setProducts(new ArrayList<>()); // Empty list

        Product savedProduct = new Product();
        savedProduct.setProductId(1L);
        savedProduct.setProductName("Test Product");

        ProductDTO expectedDTO;
        expectedDTO = new ProductDTO(1L, "Test Product", "CODE123", "image.png", "Desc", 10,
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(90.0), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), null);

        when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepo.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.productToProductDTO(savedProduct)).thenReturn(expectedDTO);

        // Act
        ProductDTO result = productService.addProduct(categoryId, expectedDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.productId());
        verify(productRepo).save(any(Product.class));
    }

    @Test
    void getProductById_shouldThrowException_whenNotFound() {
        // Arrange
        when(productRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void deleteProduct_shouldDeleteAndPublishEvent() {
        // Arrange
        Product product = new Product();
        product.setProductId(1L);
        product.setItemCode("ITEM001");

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepo).delete(product);
        verify(eventProducer).publish(eq("product_events"), any());
    }
}
