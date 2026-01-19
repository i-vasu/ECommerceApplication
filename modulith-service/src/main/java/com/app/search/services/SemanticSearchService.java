package com.app.search.services;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.app.media.ImageService;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Semantic Search Service using Deep Java Library for visual embeddings
 * Used for visual similarity search (find similar products by image)
 */
@Service
public class SemanticSearchService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SemanticSearchService.class);

    @Autowired
    private ImageService imageService;

    private ZooModel<Image, float[]> embeddingModel;
    private boolean modelLoaded = false;

    @PostConstruct
    public void init() {
        try {
            // Use the standard image classification model which generates embeddings
            Criteria<Image, float[]> criteria = Criteria.builder()
                    .setTypes(Image.class, float[].class)
                    .optEngine("PyTorch")
                    .optFilter("backbone", "resnet50")
                    .build();

            this.embeddingModel = criteria.loadModel();
            this.modelLoaded = true;
            log.info("Visual Embedding Model loaded successfully");
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            log.warn("Failed to load Embedding Model: {}. Visual search will be disabled.", e.getMessage());
            this.modelLoaded = false;
        }
    }

    /**
     * Generate an embedding vector for an image
     * 
     * @param imageStream the input image stream
     * @return float array embedding, or null if model not loaded
     */
    public float[] generateImageEmbedding(InputStream imageStream) {
        if (!modelLoaded || embeddingModel == null) {
            log.warn("Model not initialized, returning null embedding");
            return null;
        }

        try (Predictor<Image, float[]> predictor = embeddingModel.newPredictor()) {
            Image image = ImageFactory.getInstance().fromInputStream(imageStream);
            return predictor.predict(image);
        } catch (IOException | TranslateException e) {
            log.error("Error generating embedding: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert vector to string for storage/debug
     */
    public String vectorToString(float[] vector) {
        if (vector == null) {
            return "null";
        }
        return Arrays.toString(vector);
    }

    /**
     * Calculate cosine similarity between two embeddings
     */
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            normA += vec1[i] * vec1[i];
            normB += vec2[i] * vec2[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
