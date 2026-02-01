package com.app.discovery.domain.services;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.repositories.ProductRepo;
import com.app.discovery.domain.repositories.ProductEmbeddingRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class VisualSearchService {

    private final com.app.discovery.domain.repositories.jdbc.DiscoveryJdbcRepo discoveryRepo;
    private final ProductRepo productRepo;
    private final RestClient restClient;
    private final SemanticSearchService semanticSearchService;
    private final ProductEmbeddingRepo embeddingRepo;
    private final com.app.catalog.mappers.ProductMapper productMapper;

    // DJL Model details (ResNet50 is standard)
    private ZooModel<Image, float[]> model;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            // Initialize model on startup
            this.model = loadModel();
        } catch (Exception e) {
            log.error("Failed to load Vision Model: {}", e.getMessage());
        }
    }

    private ZooModel<Image, float[]> loadModel() throws ModelException, IOException {
        // Define a translator for image -> float[] (embedding)
        Translator<Image, float[]> translator = new Translator<Image, float[]>() {
            @Override
            public NDList processInput(TranslatorContext ctx, Image input) {
                var array = input.toNDArray(ctx.getNDManager());
                try {
                    array = new Resize(224, 224).transform(array);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to resize image", e);
                }
                return new NDList(array.div(255.0f).transpose(2, 0, 1)); // CHW format
            }

            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                return list.singletonOrThrow().toFloatArray();
            }

            @Override
            public Batchifier getBatchifier() {
                return Batchifier.STACK;
            }
        };

        Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optArtifactId("resnet50")
                .optEngine("PyTorch")
                .optOptions(Map.of("layers", "50"))
                .optTranslator(translator)
                .optProgress(new ProgressBar())
                .build();

        return ModelZoo.loadModel(criteria);
    }

    public List<ProductDTO> searchByImage(byte[] imageBytes) {
        if (model == null) {
            log.error("Model not initialized. Skipping visual search.");
            return new ArrayList<>();
        }

        try {
            var img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
            var embedding = extractFeatureVector(img);
            if (embedding == null)
                return new ArrayList<>();

            return findSimilarProducts(embedding);

        } catch (Exception e) {
            log.error("Visual Search failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private float[] extractFeatureVector(Image img) throws TranslateException {
        try (Predictor<Image, float[]> predictor = model.newPredictor()) {
            return predictor.predict(img);
        }
    }

    private List<ProductDTO> findSimilarProducts(float[] embedding) {
        var vectorStr = Arrays.toString(embedding);

        try {
            List<Map<String, Object>> results = discoveryRepo.findSimilarProducts(vectorStr, 5);
            return results.stream().map(row -> new ProductDTO(
                    (Long) row.get("product_id"),
                    (String) row.get("product_name"),
                    (String) row.get("item_code"),
                    (String) row.get("image"),
                    (String) row.get("description"),
                    0,
                    row.get("price") != null ? ((Number) row.get("price")).doubleValue() : 0.0,
                    0.0,
                    row.get("price") != null ? ((Number) row.get("price")).doubleValue() : 0.0,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    null,
                    new java.util.HashMap<>(),
                    null,
                    null)).toList();
        } catch (Exception e) {
            log.error("Failed to find similar products: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<ProductDTO> searchByImage(java.io.InputStream imageStream) {
        try {
            var embedding = semanticSearchService.generateImageEmbedding(imageStream);

            if (embedding == null) {
                return List.of();
            }

            var productIds = embeddingRepo.searchSimilarProducts(embedding, 10);

            if (productIds.isEmpty()) {
                return List.of();
            }

            List<com.app.catalog.entities.Product> products = productRepo.findAllById(productIds);

            return products.stream()
                    .map(productMapper::productToProductDTO)
                    .toList();

        } catch (Exception e) {
            throw new com.app.core.APIException("Failed to process image: " + e.getMessage());
        }
    }

    public void updateProductVector(Long productId, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank())
            return;

        try {
            var imageBytes = restClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .body(byte[].class);

            if (imageBytes != null) {
                var img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
                var embedding = extractFeatureVector(img);

                if (embedding != null) {
                    var vectorStr = Arrays.toString(embedding);
                    discoveryRepo.updateFeatureVector(productId, vectorStr);
                    log.info("Updated Visual Embedding for Product ID: {}", productId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to update product vector for ID {}: {}", productId, e.getMessage());
        }
    }
}
