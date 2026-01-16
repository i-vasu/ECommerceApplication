package com.app.controllers;

import com.app.entites.CustomDesign;
import com.app.repositories.CustomDesignRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/designs")
public class CustomDesignController {

    @Autowired
    private CustomDesignRepo designRepo;

    @PostMapping("/public")
    public ResponseEntity<CustomDesign> saveDesign(@RequestBody CustomDesign design) {
        if (design.getCreatedAt() == null)
            design.setCreatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(designRepo.save(design));
    }

    @GetMapping("/public/user/{userId}")
    public ResponseEntity<List<CustomDesign>> getUserDesigns(@PathVariable Long userId) {
        return ResponseEntity.ok(designRepo.findByUserId(userId));
    }

    @Autowired
    private com.app.services.AiDesignService aiService;

    @GetMapping("/public/{designId}")
    public ResponseEntity<CustomDesign> getDesign(@PathVariable Long designId) {
        return ResponseEntity.ok(designRepo.findById(designId)
                .orElseThrow(() -> new RuntimeException("Design not found")));
    }

    @PostMapping("/public/generate")
    public ResponseEntity<String> generateDesign(@RequestBody String prompt) {
        // Warning: This costs money (OpenAI API). Protect with Rate Limiting in Prod.
        return ResponseEntity.ok(aiService.generateSareeDesign(prompt));
    }

    @GetMapping(value = "/public/generate/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public reactor.core.publisher.Flux<String> generateDesignStream(@RequestParam String prompt) {
        return aiService.generateSareeDesignStream(prompt);
    }

    @PostMapping("/public/tryon")
    public ResponseEntity<String> virtualTryOn(@RequestBody java.util.Map<String, String> request) {
        // Expected: { "userPhotoUrl": "...", "sareeImageUrl": "..." }
        String resultUrl = aiService.generateVirtualTryOn(
                request.get("userPhotoUrl"),
                request.get("sareeImageUrl"));
        return ResponseEntity.ok(resultUrl);
    }
}
