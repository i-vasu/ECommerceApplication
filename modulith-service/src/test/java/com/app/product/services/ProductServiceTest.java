package com.app.product.services;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.app.core.ResourceNotFoundException;
import com.app.product.entities.Category;
import com.app.product.entities.Product;
import com.app.product.mappers.ProductMapper;
import com.app.product.payloads.ProductDTO;
import com.app.product.repositories.CategoryRepo;
import com.app.product.repositories.ProductRepo;

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
        expectedDTO = new ProductDTO(1L, "Test Product", "CODE123", "image.png", "Desc", 10, 100.0, 10.0, 90.0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);

        when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepo.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.productToProductDTO(savedProduct)).thenReturn(expectedDTO);

        // Act
        ProductDTO result = productService.addProduct(categoryId, product);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.productId());
        verify(productRepo).save(product);
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

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepo).delete(product);
        // Verify Redis publish
        verify(redisTemplate).convertAndSend(eq("product-sync-topic"), any(String.class));
    }
}
