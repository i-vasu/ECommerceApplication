package com.app.search.controllers;

import com.app.product.payloads.ProductDTO;
import com.app.search.services.SemanticSearchService;
import com.app.search.repositories.ProductEmbeddingRepo;
import com.app.product.repositories.ProductRepo;
import com.app.core.APIException;
import com.app.product.mappers.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/search/visual")
public class VisualSearchController implements VisualSearchApi {

    @Autowired
    private SemanticSearchService semanticSearchService;
    @Autowired
    private ProductEmbeddingRepo embeddingRepo;
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private ProductMapper productMapper;

    @Override
    public ResponseEntity<List<ProductDTO>> searchByImage(@RequestPart("image") MultipartFile image) {
        try {
            // 1. Generate Embedding
            float[] embedding = semanticSearchService.generateImageEmbedding(image.getInputStream());

            if (embedding == null) {
                return ResponseEntity.ok(List.of());
            }

            // 2. Search Vector DB
            List<Long> productIds = embeddingRepo.searchSimilarProducts(embedding, 10);

            if (productIds.isEmpty()) {
                throw new APIException("No similar products found");
            }

            // 3. Fetch Products
            List<com.app.product.entites.Product> products = productRepo.findAllById(productIds);

            // Preserve order roughly (though findAllById doesn't guarantee it, we could
            // re-sort)
            // For now, returning standard list
            List<ProductDTO> dtos = products.stream()
                    .map(productMapper::productToProductDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (IOException e) {
            throw new APIException("Failed to process image: " + e.getMessage());
        }
    }
}
