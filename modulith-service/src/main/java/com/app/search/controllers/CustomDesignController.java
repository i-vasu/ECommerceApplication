package com.app.search.controllers;

import com.app.search.entities.CustomDesign;
import com.app.search.repositories.CustomDesignRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/search/custom-design")
public class CustomDesignController implements CustomDesignApi {

    @Autowired
    private CustomDesignRepo designRepo;

    @Autowired
    private com.app.search.services.AiDesignService aiService;

    @Override
    public ResponseEntity<CustomDesign> saveDesign(@RequestBody CustomDesign design) {
        if (design.getCreatedAt() == null)
            design.setCreatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(designRepo.save(design));
    }

    @Override
    public ResponseEntity<List<CustomDesign>> getUserDesigns(@PathVariable Long userId) {
        return ResponseEntity.ok(designRepo.findByUserId(userId));
    }

    @Override
    public ResponseEntity<CustomDesign> getDesign(@PathVariable Long designId) {
        return ResponseEntity.ok(designRepo.findById(designId)
                .orElseThrow(() -> new RuntimeException("Design not found")));
    }

    @Override
    public ResponseEntity<String> generateDesign(@RequestBody String prompt) {
        // Warning: This costs money (OpenAI API). Protect with Rate Limiting in Prod.
        return ResponseEntity.ok(aiService.generateSareeDesign(prompt));
    }

    @Override
    public reactor.core.publisher.Flux<String> generateDesignStream(@RequestParam String prompt) {
        return aiService.generateSareeDesignStream(prompt);
    }

    @Override
    public ResponseEntity<String> virtualTryOn(@RequestBody java.util.Map<String, String> request) {
        // Expected: { "userPhotoUrl": "...", "sareeImageUrl": "..." }
        String resultUrl = aiService.generateVirtualTryOn(
                request.get("userPhotoUrl"),
                request.get("sareeImageUrl"));
        return ResponseEntity.ok(resultUrl);
    }
}
