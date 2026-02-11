package com.app.discovery.domain.services;

import com.app.catalog.ProductService;
import com.app.catalog.entities.Category;
import com.app.catalog.entities.Product;
import com.app.catalog.repositories.CategoryRepo;
import com.app.discovery.domain.entities.CustomDesign;
import com.app.discovery.domain.repositories.CustomDesignRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomDesignService {

    private final CustomDesignRepo designRepo;
    private final ProductService productService;
    private final CategoryRepo categoryRepo;

    @Transactional
    public CustomDesign saveDesign(CustomDesign design) {
        if (design.getCreatedAt() == null) {
            design.setCreatedAt(LocalDateTime.now());
        }
        return designRepo.save(design);
    }

    @Transactional(readOnly = true)
    public List<CustomDesign> getUserDesigns(Long userId) {
        return designRepo.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public CustomDesign getDesign(Long designId) {
        return designRepo.findById(designId)
                .orElseThrow(() -> new RuntimeException("Design not found"));
    }

    @Transactional
    public Object convertToProduct(Long designId) {
        CustomDesign design = getDesign(designId);

        // Find or create 'Custom Designs' category
        Category category = categoryRepo.findByCategoryName("Custom Designs");
        if (category == null) {
            category = new Category();
            category.setCategoryName("Custom Designs");
            category = categoryRepo.save(category);
        }

        Product product = new Product();
        String name = "AI Custom Design: " + (design.getPrompt().length() > 30
                ? design.getPrompt().substring(0, 30) + "..."
                : design.getPrompt());

        product.setProductName(name);
        product.setItemCode("AI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        product.setDescription(
                "An exclusive artisanal creation designed via AI interaction. Prompt: " + design.getPrompt());
        product.setImage(design.getImageUrl());
        product.setPrice(BigDecimal.valueOf(14999.00));
        product.setDiscount(BigDecimal.ZERO);
        product.setQuantity(1);
        product.setCustomizable(true);
        product.setTags(List.of("AI", "Custom", "Exclusive"));

        // Persist via catalog service to ensure all events and business logic run
        return productService.addProduct(category.getCategoryId(), product);
    }
}
