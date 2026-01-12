package com.app.services;

import com.app.entites.Product;
import com.app.payloads.ProductDTO;
import com.app.payloads.ProductResponse;
import com.app.repositories.CartRepo;
import com.app.repositories.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductServiceImplTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private CartRepo cartRepo;

    @Mock
    private CartService cartService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllProducts_Success() {
        Product product = new Product();
        product.setProductId(1L);
        product.setProductName("Test Product");
        product.setImage("123456789_test.jpg");

        Page<Product> page = new PageImpl<>(Collections.singletonList(product));
        when(productRepo.findAll(any(Pageable.class))).thenReturn(page);

        ProductDTO dto = new ProductDTO();
        dto.setProductName("Test Product");
        when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(dto);
        when(minioService.getFileUrl(anyString())).thenReturn("http://presigned-url.com");

        ProductResponse response = productService.getAllProducts(0, 10, "productId", "asc");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("http://presigned-url.com", response.getContent().get(0).getImage());
    }

    @Test
    void testDeleteProduct_EvictsCache() {
        Product product = new Product();
        product.setProductId(1L);
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));

        String result = productService.deleteProduct(1L);

        assertEquals("Product with productId: 1 deleted successfully !!!", result);
        verify(productRepo, times(1)).delete(product);
    }
}
