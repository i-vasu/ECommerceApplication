package com.app.catalog;

import com.app.core.APIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class MediaService {

    @Value("${upload.path:uploads/}")
    private String uploadPath;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public String storeFile(InputStream inputStream, String originalFilename) {
        try {
            Path root = Paths.get(uploadPath);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            String filename = UUID.randomUUID().toString() + "_" + originalFilename;
            Files.copy(inputStream, root.resolve(filename));

            return baseUrl + "/uploads/" + filename;
        } catch (IOException e) {
            throw new APIException("Could not store file: " + e.getMessage());
        }
    }
}
