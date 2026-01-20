package com.app.search.services;

import com.app.product.entites.Category;
import com.app.product.entites.Product;
import com.app.product.entites.ProductVariant;
import com.app.product.repositories.CategoryRepo;
import com.app.product.repositories.ProductRepo;
import com.app.product.repositories.ProductVariantRepo;
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
    private ProductVariantRepo variantRepo;
    @Autowired
    private com.app.product.repositories.ProductMediaRepo mediaRepo;
    @Autowired
    private com.app.search.repositories.ProductEmbeddingRepo embeddingRepo;
    @Autowired
    private ImageService imageService;
    @Autowired
    private VisualSearchService visualSearchService;
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
        
        // ... (Keep existing simplified logic if needed, but processItemWithMedia is the main one now)
        // Re-using processItemWithMedia logic structure
        Category category = categoryRepo.findByCategoryName(categoryName);
        if (category == null) {
            category = new Category();
            category.setCategoryName(categoryName);
            category = categoryRepo.save(category);
        }

        Product product = productRepo.findByItemCode(itemCode);
        if (product == null) {
            product = new Product();
            product.setItemCode(itemCode);
        }

        product.setProductName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setSpecialPrice(price);
        product.setCategory(category);
        if (imageUrl != null) product.setImage(imageUrl);

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
        Integer hasVariants = (Integer) itemData.getOrDefault("has_variants", 0);
        String variantOf = (String) itemData.get("variant_of");

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
        Product product;
        if (variantOf != null && !variantOf.isEmpty()) {
            // This is a variant item in ERPNext
            // We should ensure the parent exists, or treat this as a standalone if parent missing?
            // For now, let's treat it as a variant of the Parent Product
            Product parent = productRepo.findByItemCode(variantOf);
            if (parent != null) {
                // Upsert Variant
                ProductVariant variant = variantRepo.findByItemCode(itemCode);
                if (variant == null) {
                    variant = new ProductVariant();
                    variant.setItemCode(itemCode);
                    variant.setProduct(parent);
                }
                // extract attributes from name e.g. "Shirt - Red - L"
                // simplified logic:
                // variant.setPrice(price); // Logic Error: Variant has no price field yet
                variant.setStockQuantity(10); // Default, strict sync happens in stock flow
                variantRepo.save(variant);
                log.info("Synced Variant {} for Parent {}", itemCode, variantOf);
                return; // Done for variant
            }
            // If parent not found, we might want to create it or skip.
            // Let's fall through and create it as a Product for safety so we don't lose data
        }

        product = productRepo.findByItemCode(itemCode);
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
             // Don't overwrite existing if null
             if (product.getImage() == null) product.setImage("default.png");
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
                        media.setType("IMAGE"); 

                        // Generate BlurHash
                        try {
                            if (fullUrl != null) {
                                // Async generation (blocking here for simplicity in this flow, or use future)
                                // Ideally, we download the bytes once, resize, and hash.
                                // Here we just fetch stream for hash.
                                java.io.InputStream is = restClient.get()
                                    .uri(fullUrl)
                                    .retrieve()
                                    .body(java.io.InputStream.class);

                                if (is != null) {
                                    java.util.concurrent.CompletableFuture<String> hashFuture = imageService.generateBlurHash(is);
                                    String hash = hashFuture.join(); // Wait for completion
                                    if (hash != null) {
                                        media.setBlurHash(hash);
                                    }
                                }
                            }
                        } catch (Exception e) {
                             log.warn("Failed to generate BlurHash for {}: {}", itemCode, e.getMessage());
                        }

                        mediaRepo.save(media);
                    }
                    log.info("Synced {} media items for Product: {}", files.size(), itemCode);
                }
            }

            // 5. Generate Visual Embedding
            String currentImage = product.getImage();
            if (currentImage != null && !currentImage.isEmpty()) {
                try {
                    String fullImageUrl = currentImage.startsWith("http") ? currentImage
                            : credentialProvider.getBaseUrl() + currentImage;

                    // Delegate to VisualSearchService (Real Implementation)
                    visualSearchService.updateProductVector(product.getProductId(), fullImageUrl);
                    
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
