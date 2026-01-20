package com.app.search.services;

import com.app.product.payloads.ProductDTO;
import com.app.product.entites.Product;
import com.app.product.repositories.ProductRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.Batchifier;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VisualSearchService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductRepo productRepo;

    // DJL Model details (ResNet50 is standard)
    private ZooModel<Image, float[]> model;

    public VisualSearchService() {
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
                // Resize to 224x224 (ResNet standard) and convert to Tensor
                NDArray array = input.toNDArray(ctx.getNDManager());
                try {
                     // Resize expects NDArray
                     array = new Resize(224, 224).transform(array);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to resize image", e);
                }
                return new NDList(array.div(255.0f).transpose(2, 0, 1)); // CHW format
            }

            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                // Return the flatten embedding
                return list.singletonOrThrow().toFloatArray();
            }
            
            // Required for interface implementation
            @Override
            public Batchifier getBatchifier() {
               return Batchifier.STACK;
            }
        };

        Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optModelUrls("djl://ai.djl.pytorch/resnet50") // Auto-download ResNet50
                .optEngine("PyTorch")
                .optOptions(Map.of("layers", "50")) // Fixed: optConfig -> optOptions
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
            Image img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
            
            float[] embedding = extractFeatureVector(img);
            if (embedding == null) return new ArrayList<>();

            // Perform Vector Search in Postgres (ParadeDB/pgvector)
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
        // pgvector query: SELECT * FROM products ORDER BY feature_vector <-> '[0.1, 0.2, ...]' LIMIT 5
        
        // Convert array to string format for SQL: '[0.1,0.2,...]'
        String vectorStr = Arrays.toString(embedding); 

        // Native Query
        String sql = "SELECT product_id, product_name, item_code, price, image, description " +
                     "FROM products " +
                     "ORDER BY feature_vector <-> ?::vector LIMIT 5";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ProductDTO dto = new ProductDTO();
            dto.setProductId(rs.getLong("product_id"));
            dto.setProductName(rs.getString("product_name"));
            dto.setItemCode(rs.getString("item_code"));
            dto.setPrice(rs.getDouble("price"));
            dto.setImage(rs.getString("image"));
            dto.setDescription(rs.getString("description"));
            return dto;
        }, vectorStr);
    }
    
    @Autowired
    private org.springframework.web.client.RestClient restClient;

    // Method to be called during Product Sync to generate and save vector
    public void updateProductVector(Long productId, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        try {
            // Calculate embeddings
             byte[] imageBytes = restClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .body(byte[].class);

            if (imageBytes != null) {
                Image img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
                float[] embedding = extractFeatureVector(img);
                
                if (embedding != null) {
                    // Update DB using Native Query
                    String vectorStr = Arrays.toString(embedding);
                    String sql = "UPDATE products SET feature_vector = ?::vector WHERE product_id = ?";
                    jdbcTemplate.update(sql, vectorStr, productId);
                    log.info("Updated Visual Embedding for Product ID: {}", productId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to update product vector for ID {}: {}", productId, e.getMessage());
        }
    }
}
