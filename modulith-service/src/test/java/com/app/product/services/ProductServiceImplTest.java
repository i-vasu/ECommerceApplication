package com.app.product.services;

import com.app.catalog.entities.Product;
import com.app.catalog.entities.Category;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.repositories.CategoryRepo;
import com.app.core.ResourceNotFoundException;
import com.app.catalog.services.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @Mock
    private ProductRepo productRepo;
    @Mock
    private CategoryRepo categoryRepo;
    @Mock
    private ModelMapper modelMapper;

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

        testProduct = new Product();
        testProduct.setProductId(1L);
        testProduct.setProductName("Silk Saree");
        testProduct.setPrice(BigDecimal.valueOf(5000.0));
        testProduct.setQuantity(10);

        testProductDTO = new ProductDTO(1L, "Silk Saree", "Silk", 10, BigDecimal.valueOf(5000.0), 0.0, BigDecimal.valueOf(5000.0), "silk-saree.jpg", "ITEM001");
    }

    @Test
    void testAddProduct_Success() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(testCategory));
        when(modelMapper.map(any(ProductDTO.class), eq(Product.class))).thenReturn(testProduct);
        when(productRepo.save(any(Product.class))).thenReturn(testProduct);
        when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(testProductDTO);

        ProductDTO saved = productService.addProduct(1L, testProductDTO);

        assertNotNull(saved);
        assertEquals("Silk Saree", saved.productName());
        verify(productRepo).save(any());
    }

    @Test
    void testGetProductById_Found() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(testProduct));
        when(modelMapper.map(testProduct, ProductDTO.class)).thenReturn(testProductDTO);

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
        when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(testProductDTO);

        ProductDTO updated = productService.updateProduct(1L, testProductDTO);

        assertNotNull(updated);
        verify(productRepo).save(any());
    }

    @Test
    void testDeleteProduct() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(testProduct));
        
        productService.deleteProduct(1L);
        
        verify(productRepo).delete(testProduct);
    }
}
