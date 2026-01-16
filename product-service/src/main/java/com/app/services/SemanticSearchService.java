package com.app.services;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class SemanticSearchService {

    private ZooModel<Image, float[]> embeddingModel;

    public SemanticSearchService() {
        try {
            // Load CLIP model (visual) from HuggingFace or ModelZoo
            // For simplicity, we use a standard ResNet50 embedding model first as a
            // placeholder
            // or specific CLIP implementation if available in standard Zoo.
            // Using a generic feature extraction model for visual similarity.
            Criteria<Image, float[]> criteria = Criteria.builder()
                    .optApplication(Application.CV.IMAGE_FEATURE_EXTRACTION)
                    .setTypes(Image.class, float[].class)
                    .optFilter("backbone", "resnet50")
                    .optProgress(new ProgressBar())
                    .build();

            this.embeddingModel = ModelZoo.loadModel(criteria);
            log.info("Visual Embedding Model loaded successfully");
        } catch (IOException | ModelException e) {
            log.error("Failed to load Embedding Model: {}", e.getMessage());
        }
    }

    public float[] generateImageEmbedding(InputStream imageStream) {
        if (embeddingModel == null) {
            throw new IllegalStateException("Model not initialized");
        }
        try (Predictor<Image, float[]> predictor = embeddingModel.newPredictor()) {
            Image image = ImageFactory.getInstance().fromInputStream(imageStream);
            return predictor.predict(image);
        } catch (IOException | TranslateException e) {
            log.error("Error generating embedding: {}", e.getMessage());
            throw new RuntimeException("Embedding generation failed", e);
        }
    }

    public String vectorToString(float[] vector) {
        return Arrays.toString(vector);
    }
}
