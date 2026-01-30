package com.app.discovery.domain.controllers;

import com.app.catalog.payloads.ProductDTO;
import com.app.discovery.domain.services.VisualSearchService;
import com.app.core.APIException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/search/visual")
@RequiredArgsConstructor
@SecurityRequirement(name = "E-Commerce Application")
public class VisualSearchController implements VisualSearchApi {

    private final VisualSearchService visualSearchService;

    @Override
    public ResponseEntity<List<ProductDTO>> searchByImage(@RequestPart("image") MultipartFile image) {
        // Harden: Validate image size
        if (image.getSize() > com.app.config.AppConstants.MAX_IMAGE_SIZE_BYTES) {
            throw new APIException("Image size exceeds maximum allowed limit (5MB)");
        }

        // Harden: Validate content type
        String contentType = image.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new APIException("Only JPEG and PNG images are supported for visual search");
        }

        try {
            return ResponseEntity.ok(visualSearchService.searchByImage(image.getInputStream()));
        } catch (IOException e) {
            throw new APIException("Failed to process image: " + e.getMessage());
        }
    }
}
