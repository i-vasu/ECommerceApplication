package com.app.catalog;

import com.app.payloads.ProductDTO;
import com.app.services.SemanticSearchService;
import com.app.repositories.ProductEmbeddingRepo;
import com.app.repositories.ProductRepo;
import com.app.exceptions.APIException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/products/search")
public class VisualSearchController {

    @Autowired
    private SemanticSearchService semanticSearchService;

    @Autowired
    private ProductEmbeddingRepo embeddingRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping("/visual")
    public ResponseEntity<List<ProductDTO>> searchByImage(@RequestParam("image") MultipartFile image) {
        try {
            // 1. Generate Embedding
            float[] embedding = semanticSearchService.generateImageEmbedding(image.getInputStream());

            // 2. Search Vector DB
            List<Long> productIds = embeddingRepo.searchSimilarProducts(embedding, 10);

            if (productIds.isEmpty()) {
                throw new APIException("No similar products found");
            }

            // 3. Fetch Products
            List<com.app.entites.Product> products = productRepo.findAllById(productIds);

            // Preserve order roughly (though findAllById doesn't guarantee it, we could
            // re-sort)
            // For now, returning standard list
            List<ProductDTO> dtos = products.stream()
                    .map(p -> modelMapper.map(p, ProductDTO.class))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (IOException e) {
            throw new APIException("Failed to process image: " + e.getMessage());
        }
    }
}
