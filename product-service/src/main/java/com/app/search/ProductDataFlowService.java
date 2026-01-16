package com.app.search;

import com.app.entites.Category;
import com.app.entites.Product;
import com.app.repositories.CategoryRepo;
import com.app.repositories.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to handle data flow between databases and sync mechanisms.
 * Currently uses ProductService for logic, this class encapsulates the specific
 * flow logic
 * if we want to separate it from pure CRUD.
 */
@Service
@RequiredArgsConstructor
public class ProductDataFlowService {

    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final com.app.repositories.ProductMediaRepo mediaRepo;
    private final com.app.services.ImageService imageService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${erpnext.api.url}")
    private String erpNextUrl;

    @org.springframework.beans.factory.annotation.Value("${erpnext.api.key}")
    private String apiKey;

    @org.springframework.beans.factory.annotation.Value("${erpnext.api.secret}")
    private String apiSecret;

    /**
     * Flow: ERPNext -> Java App -> Postgres
     * This method saves the "Master" data from ERPNext into our local "Read"
     * database (Postgres).
     */
    @Transactional
    public void upsertProductFromERPNext(String itemCode, String name, String description, Double price,
            String imageUrl, String categoryName) {

        // 1. Resolve Category
        Category category = categoryRepo.findByCategoryName(categoryName);
        if (category == null) {
            category = new Category();
            category.setCategoryName(categoryName);
            category = categoryRepo.save(category);
        }

        // 2. Resolve Product
        Product product = productRepo.findByItemCode(itemCode);
        if (product == null) {
            product = new Product();
            product.setItemCode(itemCode);
        }

        // 3. Update Fields (Data Flow: ERPMaster -> LocalCopy)
        product.setProductName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setSpecialPrice(price); // Fallback: sales rules apply later
        product.setCategory(category);

        // Just store the URL, image downloading happens asynchronously if needed
        if (imageUrl != null) {
            product.setImage(imageUrl);
        }

        return productRepo.save(product);
    }

    @Transactional
    public void processItemWithMedia(Map<String, Object> itemData) {
        String itemCode = (String) itemData.get("name");
        Product product = productRepo.findByItemCode(itemCode);

        // 2. Fetch Attached Files
        try {
            String filesUrl = erpNextUrl.replace("/Item", "/File") + "?filters=[[\"attached_to_name\",\"=\",\""
                    + itemCode + "\"]]&fields=[\"file_url\",\"is_private\",\"file_name\"]";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.exchange(filesUrl,
                    org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                java.util.List<java.util.Map<String, Object>> files = (java.util.List<java.util.Map<String, Object>>) response
                        .getBody().get("data");
                if (files != null) {
                    mediaRepo.deleteByProduct(product); // Clear old media

                    for (java.util.Map<String, Object> file : files) {
                        String fileUrl = (String) file.get("file_url");
                        if (fileUrl == null)
                            continue;

                        // Fix URL if relative
                        String fullUrl = fileUrl.startsWith("http") ? fileUrl : erpNextUrl.split("/api")[0] + fileUrl;

                        com.app.entites.ProductMedia media = new com.app.entites.ProductMedia();
                        media.setProduct(product);
                        media.setUrl(fullUrl);
                        media.setType("IMAGE"); // Auto-detect video later

                        // Async BlurHash & Resize
                        // In real flow, we download bytes here.
                        // For this step, we just generating BlurHash from the URL if possible, or skip
                        // To keep it simple, we save the media record. processing happens in background
                        // or seperate flow.

                        // Trigger ImageService (Placeholder logic)
                        // imageService.generateBlurHash(...)

                        mediaRepo.save(media);
                    }
                }
            }

            // 3. Generate Embedding for the main image (Visual Search)
            // We do this AFTER processing all files to ensuring main image is handled
            // For simplicity, we re-use the main 'image' field or the first media
            if (image != null && !image.isEmpty()) {
                try {
                    String fullImageUrl = image.startsWith("http") ? image : erpNextUrl.split("/api")[0] + image;
                    byte[] imageBytes = restTemplate.getForObject(fullImageUrl, byte[].class);
                    if (imageBytes != null) {
                        try (java.io.InputStream is = new java.io.ByteArrayInputStream(imageBytes)) {
                            float[] embedding = semanticSearchService.generateImageEmbedding(is);
                            embeddingRepo.saveEmbedding(product.getProductId(), embedding);
                            log.info("Generated visual coordinates for product: {} (Dim: {})", itemCode,
                                    embedding.length);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Visual Embedding failed for {}: {}", itemCode, e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error syncing media for " + itemCode + ": " + e.getMessage());
        }
    }

    private Double getDouble(Object obj) {
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        return 0.0;
    }
}
