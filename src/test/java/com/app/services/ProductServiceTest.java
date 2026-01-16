package com.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.app.entites.Category;
import com.app.entites.Product;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.ProductDTO;
import com.app.repositories.CategoryRepo;
import com.app.repositories.ProductRepo;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private CategoryRepo categoryRepo;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    public void setup() {
        category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Electronics");

        product = new Product();
        product.setProductName("Laptop");
        product.setDescription("High performance laptop");
        product.setPrice(1000.0);
        product.setDiscount(10.0);

        productDTO = new ProductDTO();
        productDTO.setProductName("Laptop");
        productDTO.setDescription("High performance laptop");
        productDTO.setPrice(1000.0);
        productDTO.setDiscount(10.0);
    }

    @Test
    public void addProduct_WhenProductDoesNotExist_ShouldSaveProduct() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
        when(productRepo.existsByProductNameAndDescriptionAndCategory(product.getProductName(), product.getDescription(), category))
                .thenReturn(false);
        when(productRepo.save(any(Product.class))).thenReturn(product);
        when(modelMapper.map(product, ProductDTO.class)).thenReturn(productDTO);

        ProductDTO savedProduct = productService.addProduct(1L, product);

        assertNotNull(savedProduct);
        assertEquals(product.getProductName(), savedProduct.getProductName());
        verify(productRepo).existsByProductNameAndDescriptionAndCategory(product.getProductName(), product.getDescription(), category);
        verify(productRepo).save(any(Product.class));
        // Verify we are NOT calling getProducts anymore
        // (Though we can't easily verify a method on a pojo wasn't called without spying,
        // the main point is we rely on the repo check)
    }

    @Test
    public void addProduct_WhenProductExists_ShouldThrowException() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
        when(productRepo.existsByProductNameAndDescriptionAndCategory(product.getProductName(), product.getDescription(), category))
                .thenReturn(true);

        APIException exception = assertThrows(APIException.class, () -> {
            productService.addProduct(1L, product);
        });

        assertEquals("Product already exists !!!", exception.getMessage());
        verify(productRepo).existsByProductNameAndDescriptionAndCategory(product.getProductName(), product.getDescription(), category);
        verify(productRepo, never()).save(any(Product.class));
    }
}
