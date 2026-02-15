package com.app.discovery.domain.services;

import com.app.catalog.ProductService;
import com.app.catalog.entities.Category;
import com.app.catalog.entities.Product;
import com.app.catalog.repositories.CategoryRepo;
import com.app.discovery.domain.entities.CustomDesign;
import com.app.discovery.domain.repositories.CustomDesignRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomDesignServiceTest {

    @Mock
    private CustomDesignRepo designRepo;

    @Mock
    private ProductService productService;

    @Mock
    private CategoryRepo categoryRepo;

    @InjectMocks
    private CustomDesignService designService;

    @Test
    void testSaveDesign() {
        CustomDesign design = new CustomDesign();
        design.setPrompt("A futuristic silk dress");
        when(designRepo.save(any())).thenReturn(design);

        CustomDesign saved = designService.saveDesign(design);

        assertNotNull(saved);
        verify(designRepo).save(design);
    }

    @Test
    void testConvertToProduct() {
        // Arrange
        Long designId = 1L;
        CustomDesign design = new CustomDesign();
        design.setDesignId(designId);
        design.setPrompt("Deep Sea themed Saree");
        design.setImageUrl("http://example.com/image.png");

        Category category = new Category();
        category.setCategoryId(10L);

        when(designRepo.findById(designId)).thenReturn(Optional.of(design));
        when(categoryRepo.findByCategoryName("Custom Designs")).thenReturn(category);
        when(productService.addProduct(eq(10L), any(com.app.catalog.payloads.ProductDTO.class))).thenReturn(new com.app.catalog.payloads.ProductDTO(
                1L, "Test Product", "CODE", "img.png", "Desc", 10, java.math.BigDecimal.valueOf(100.0), java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(100.0),
                java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList(), 5.0
        ));

        // Act
        Object result = designService.convertToProduct(designId);

        // Assert
        assertNotNull(result);
        verify(productService).addProduct(eq(10L), any(com.app.catalog.payloads.ProductDTO.class));
    }
}
