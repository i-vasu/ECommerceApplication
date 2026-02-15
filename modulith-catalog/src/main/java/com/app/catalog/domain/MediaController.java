package com.app.catalog.domain;

import com.app.core.APIException;
import com.app.core.payloads.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@SecurityRequirement(name = "E-Commerce Application")
public class MediaController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${upload.path:uploads/}")
    private String uploadPath;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new APIException("File is empty");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new APIException("File size exceeds maximum allowed size of 5MB");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new APIException("Invalid file type. Allowed types: JPEG, PNG, WebP, GIF");
        }

        try {
            Path root = Paths.get(uploadPath);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), root.resolve(filename));

            String fileUrl = baseUrl + "/uploads/" + filename;
            return ResponseEntity.ok(ApiResponse.success(fileUrl, "File uploaded successfully"));

        } catch (IOException e) {
            throw new APIException("Could not store file: " + e.getMessage());
        }
    }
}

