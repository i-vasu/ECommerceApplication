package com.app.search.services;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AITaggingService {

    private ZooModel<Image, Classifications> model;
    private final RestClient restClient;

    public AITaggingService(RestClient restClient) {
        this.restClient = restClient;
    }

    @PostConstruct
    public void init() {
        try {
            Criteria<Image, Classifications> criteria = Criteria.builder()
                .setTypes(Image.class, Classifications.class)
                .optArtifactId("resnet")
                .optFilter("layers", "50")
                .build();
            this.model = ModelZoo.loadModel(criteria);
            log.info("AI Tagging Model (ResNet50) loaded successfully.");
        } catch (ModelException | IOException e) {
            log.warn("Failed to load AI Tagging Model: {}", e.getMessage());
        }
    }

    public List<String> generateTags(String imageUrl) {
        if (model == null || imageUrl == null) return List.of();

        try {
            byte[] imageBytes = restClient.get()
                .uri(imageUrl)
                .retrieve()
                .body(byte[].class);

            if (imageBytes == null) return List.of();

            Image img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
            
            try (Predictor<Image, Classifications> predictor = model.newPredictor()) {
                Classifications classifications = predictor.predict(img);
                return classifications.topK(5).stream()
                    .map(Classifications.Classification::getClassName)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("AI Tagging failed for {}: {}", imageUrl, e.getMessage());
            return List.of();
        }
    }
}
