package com.app.search.controllers;

import com.app.product.payloads.ProductDTO;
import com.app.search.services.SemanticSearchService;
import com.app.search.repositories.ProductEmbeddingRepo;
import com.app.product.repositories.ProductRepo;
import com.app.core.APIException;
import com.app.product.mappers.ProductMapper;
import com.app.product.entities.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/search/visual")
@RequiredArgsConstructor
@SecurityRequirement(name = "E-Commerce Application")
public class VisualSearchController implements VisualSearchApi {

    private final SemanticSearchService semanticSearchService;
    private final ProductEmbeddingRepo embeddingRepo;
    private final ProductRepo productRepo;
    private final ProductMapper productMapper;

    @Override
    public ResponseEntity<List<ProductDTO>> searchByImage(@RequestPart("image") MultipartFile image) {
        try {
            var embedding = semanticSearchService.generateImageEmbedding(image.getInputStream());

            if (embedding == null) {
                return ResponseEntity.ok(List.of());
            }

            var productIds = embeddingRepo.searchSimilarProducts(embedding, 10);

            if (productIds.isEmpty()) {
                throw new APIException("No similar products found");
            }

            List<Product> products = productRepo.findAllById(productIds);

            var dtos = products.stream()
                    .map(productMapper::productToProductDTO)
                    .toList();

            return ResponseEntity.ok(dtos);

        } catch (IOException e) {
            throw new APIException("Failed to process image: " + e.getMessage());
        }
    }
}
