package com.app.search.services;

import com.app.product.entites.Category;
import com.app.product.entites.Product;
import com.app.product.repositories.CategoryRepo;
import com.app.product.repositories.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service to handle data flow between databases and sync mechanisms.
 * Currently uses ProductService for logic, this class encapsulates the specific
 * flow logic
 * if we want to separate it from pure CRUD.
 */
import com.app.media.ImageService;
import com.app.search.services.SemanticSearchService;

@Service
public class ProductDataFlowService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductDataFlowService.class);

    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private CategoryRepo categoryRepo;
    @Autowired
    private com.app.product.repositories.ProductMediaRepo mediaRepo;
    @Autowired
    private com.app.search.repositories.ProductEmbeddingRepo embeddingRepo;
    @Autowired
    private ImageService imageService;
    @Autowired
    private SemanticSearchService semanticSearchService;
    @Autowired
    private org.springframework.web.client.RestClient restClient;

    @Autowired
    private com.app.core.multitenancy.ERPNextCredentialProvider credentialProvider;

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

        productRepo.save(product);
    }

    @Transactional
    public void processItemWithMedia(Map<String, Object> itemData) {
        String itemCode = (String) itemData.get("name");
        String name = (String) itemData.get("item_name");
        String description = (String) itemData.get("description");
        Double price = getDouble(itemData.get("standard_rate"));
        String imageUrl = (String) itemData.get("image");
        String categoryName = (String) itemData.get("item_group");

        if (categoryName == null) {
            categoryName = "Default";
        }

        // 1. Resolve Category
        Category category = categoryRepo.findByCategoryName(categoryName);
        if (category == null) {
            category = new Category();
            category.setCategoryName(categoryName);
            category = categoryRepo.save(category);
            log.info("Created new Category: {}", categoryName);
        }

        // 2. Resolve Product
        Product product = productRepo.findByItemCode(itemCode);
        if (product == null) {
            product = new Product();
            product.setItemCode(itemCode);
        }

        // 3. Update Fields (Data Flow: ERPMaster -> LocalCopy)
        product.setProductName(name);
        product.setDescription(description != null && !description.isEmpty() ? description : name);
        product.setPrice(price);
        product.setSpecialPrice(price);
        product.setCategory(category);

        // Sync Brand
        if (itemData.containsKey("brand")) {
            product.setBrand((String) itemData.get("brand"));
        }

        if (imageUrl != null) {
            product.setImage(imageUrl);
        } else {
            product.setImage("default.png");
        }

        product = productRepo.save(product);
        log.info("Upserted Product: {} in Category: {}", itemCode, categoryName);

        // 4. Fetch Attached Files
        try {
            String filesUrl = credentialProvider.getBaseUrl() + "/api/resource/File"
                    + "?filters=[[\"attached_to_name\",\"=\",\""
                    + itemCode + "\"]]&fields=[\"file_url\",\"is_private\",\"file_name\"]";

            org.springframework.http.ResponseEntity<java.util.Map> response = restClient.get()
                    .uri(filesUrl)
                    .header("Authorization",
                            "token " + credentialProvider.getApiKey() + ":" + credentialProvider.getApiSecret())
                    .retrieve()
                    .toEntity(java.util.Map.class);

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
                        String fullUrl = fileUrl.startsWith("http") ? fileUrl
                                : credentialProvider.getBaseUrl() + fileUrl;

                        com.app.product.entites.ProductMedia media = new com.app.product.entites.ProductMedia();
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
                    log.info("Synced {} media items for Product: {}", files.size(), itemCode);
                }
            }

            // 5. Generate Visual Embedding
            // We do this AFTER processing all files to ensuring main image is handled
            // For simplicity, we re-use the main 'image' field or the first media
            String currentImage = product.getImage();
            if (currentImage != null && !currentImage.isEmpty()) {
                try {
                    String fullImageUrl = currentImage.startsWith("http") ? currentImage
                            : credentialProvider.getBaseUrl() + currentImage;

                    // Use retrieve().onStatus() or body(InputStream.class)
                    // Actually, let's simplify embedding generation to use the inputstream directly
                    java.io.InputStream imageStream = restClient.get()
                            .uri(fullImageUrl)
                            .retrieve()
                            .body(java.io.InputStream.class);

                    if (imageStream != null) {
                        float[] embedding = semanticSearchService.generateImageEmbedding(imageStream);
                        if (embedding != null) {
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
