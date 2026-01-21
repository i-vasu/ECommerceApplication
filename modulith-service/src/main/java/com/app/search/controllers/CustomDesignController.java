package com.app.search.controllers;

import com.app.search.entities.CustomDesign;
import com.app.search.repositories.CustomDesignRepo;
import com.app.search.services.AiDesignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import reactor.core.publisher.Flux;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/search/custom-design")
@RequiredArgsConstructor
@SecurityRequirement(name = "E-Commerce Application")
public class CustomDesignController implements CustomDesignApi {

    private final CustomDesignRepo designRepo;
    private final AiDesignService aiService;

    @Override
    public ResponseEntity<CustomDesign> saveDesign(@RequestBody CustomDesign design) {
        if (design.getCreatedAt() == null)
            design.setCreatedAt(LocalDateTime.now());
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
    public Flux<String> generateDesignStream(@RequestParam String prompt) {
        return aiService.generateSareeDesignStream(prompt);
    }

    @Override
    public ResponseEntity<String> virtualTryOn(@RequestBody Map<String, String> request) {
        var resultUrl = aiService.generateVirtualTryOn(
                request.get("userPhotoUrl"),
                request.get("sareeImageUrl"));
        return ResponseEntity.ok(resultUrl);
    }
}
