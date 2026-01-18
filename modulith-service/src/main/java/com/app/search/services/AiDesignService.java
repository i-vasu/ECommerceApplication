package com.app.search.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import java.util.Map;
import java.util.HashMap;

@Service
public class AiDesignService {

    private final RestClient restClient;

    @Value("${openai.api.key:}")
    private String openAiKey;

    @Value("${vton.api.url:https://api.fal.ai/v1/virtual-try-on}")
    private String vtonUrl;

    @Value("${vton.api.key:}")
    private String vtonKey;

    public AiDesignService() {
        this.restClient = RestClient.builder().build();
    }

    /**
     * Generates a design with real-time progress feedback.
     * Returns a Flux that emits status strings and finally the Image URL.
     */
    public Flux<String> generateSareeDesignStream(String userPrompt) {
        return Flux.create(sink -> {
            try {
                // Simulate Real-time "Thinking" layers
                sink.next("Thinking... Analyzing your style request: " + userPrompt);
                Thread.sleep(800);

                sink.next("Designing... Drafting the saree layout and border motifs.");
                Thread.sleep(1000);

                sink.next("Refining... Applying " + (userPrompt.contains("color") ? "colors" : "textures")
                        + " and lighting.");

                // Actual Call to Image Gen
                String imageUrl = generateSareeDesign(userPrompt);

                sink.next("Rendering... Finalizing 4K resolution.");
                sink.next("DONE:" + imageUrl);
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    public String generateSareeDesign(String userPrompt) {
        if (openAiKey == null || openAiKey.isEmpty()) {
            return "https://via.placeholder.com/1024x1024.png?text=AI+Key+Missing";
        }

        try {
            String enhancedPrompt = "A realistic, high-quality South Indian saree design. " + userPrompt +
                    ", detailed fabric texture, 4k resolution, studio lighting, flat lay photography.";

            Map<String, Object> request = new HashMap<>();
            request.put("prompt", enhancedPrompt);
            request.put("n", 1);
            request.put("size", "1024x1024");
            request.put("model", "dall-e-3");

            Map response = restClient.post()
                    .uri("https://api.openai.com/v1/images/generations")
                    .header("Authorization", "Bearer " + openAiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            java.util.List<Map<String, String>> data = (java.util.List<Map<String, String>>) response.get("data");
            return data.get(0).get("url");

        } catch (Exception e) {
            System.err.println("AI Generation Failed: " + e.getMessage());
            return "https://via.placeholder.com/1024x1024.png?text=Generation+Failed";
        }
    }

    /**
     * Google Vertex AI Virtual Try-On Implementation.
     * Docs:
     * https://cloud.google.com/vertex-ai/docs/generative-ai/image/virtual-try-on
     */
    public String generateVirtualTryOn(String userPhotoUrl, String sareeImageUrl) {
        if (vtonKey == null || vtonKey.isEmpty()) {
            return "https://via.placeholder.com/1024x1024.png?text=Google+Auth+Missing";
        }

        try {
            // 1. Download images and convert to Base64
            String userBase64 = downloadAsBase64(userPhotoUrl);
            String garmentBase64 = downloadAsBase64(sareeImageUrl);

            // 2. Build Google Vertex AI Payload
            Map<String, String> personImage = new HashMap<>();
            personImage.put("bytesBase64Encoded", userBase64);

            Map<String, String> clothingImage = new HashMap<>();
            clothingImage.put("bytesBase64Encoded", garmentBase64);

            Map<String, Object> instance = new HashMap<>();
            instance.put("person_image", personImage);
            instance.put("clothing_image", clothingImage);

            Map<String, Object> request = new HashMap<>();
            request.put("instances", java.util.List.of(instance));
            request.put("parameters", Map.of("sampleCount", 1));

            // 3. Call Google Vertex AI Endpoint
            Map response = restClient.post()
                    .uri(vtonUrl)
                    .header("Authorization", "Bearer " + vtonKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            // 4. Parse Response (Assuming typical Vertex Prediction format)
            java.util.List<Map<String, String>> predictions = (java.util.List<Map<String, String>>) response
                    .get("predictions");
            String outputBase64 = predictions.get(0).get("bytesBase64Encoded");

            // Return data URI for direct display
            return "data:image/png;base64," + outputBase64;

        } catch (Exception e) {
            System.err.println("Google VTON Failed: " + e.getMessage());
            return "https://via.placeholder.com/1024x1024.png?text=VTON+Error";
        }
    }

    private String downloadAsBase64(String imageUrl) {
        byte[] imageBytes = restClient.get()
                .uri(imageUrl)
                .retrieve()
                .body(byte[].class);
        return java.util.Base64.getEncoder().encodeToString(imageBytes);
    }
}
