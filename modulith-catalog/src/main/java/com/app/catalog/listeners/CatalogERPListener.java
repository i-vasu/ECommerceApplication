package com.app.catalog.listeners;

import com.app.catalog.entities.Category;
import com.app.catalog.entities.Product;
import com.app.catalog.entities.ProductMedia;
import com.app.catalog.entities.ProductVariant;
import com.app.catalog.repositories.CategoryRepo;
import com.app.catalog.repositories.ProductMediaRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.repositories.ProductVariantRepo;
import com.app.core.events.ERPItemSyncRequestedEvent;
import com.app.core.events.ProductCreatedEvent;
import com.app.core.events.ProductUpdatedEvent;
import com.app.core.events.ProductEnrichedEvent;
import com.app.core.multitenancy.ERPNextCredentialProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class CatalogERPListener {

    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final ProductVariantRepo variantRepo;
    private final ProductMediaRepo mediaRepo;
    private final ERPNextCredentialProvider credentialProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final RestClient.Builder restClientBuilder;

    /**
     * Handle item sync request from ERPNext webhook.
     */
    @EventListener
    @Transactional
    public void onERPItemSyncRequested(ERPItemSyncRequestedEvent event) {
        Map<String, Object> itemData = event.itemData();
        String itemCode = (String) itemData.get("name");
        log.info("Catalog: Processing ERP sync for item {}", itemCode);

        try {
            processItem(itemData);
        } catch (Exception e) {
            log.error("Catalog: Failed to process ERP sync for item {} - {}", itemCode, e.getMessage(), e);
        }
    }

    private void processItem(Map<String, Object> itemData) {
        String itemCode = (String) itemData.get("name");
        String name = (String) itemData.get("item_name");
        String description = (String) itemData.get("description");
        Double price = getDouble(itemData.get("standard_rate"));
        String imageUrl = (String) itemData.get("image");
        String categoryName = (String) itemData.get("item_group");
        String variantOf = (String) itemData.get("variant_of");

        if (categoryName == null) {
            categoryName = "Default";
        }

        // 1. Upsert Category
        var category = categoryRepo.findByCategoryName(categoryName);
        if (category == null) {
            category = new Category();
            category.setCategoryName(categoryName);
            category = categoryRepo.save(category);
            log.info("Catalog: Created new Category: {}", categoryName);
        }

        // 2. Handle Variants
        if (variantOf != null && !variantOf.isEmpty()) {
            var parent = productRepo.findByItemCode(variantOf);
            if (parent != null) {
                var variant = variantRepo.findByItemCode(itemCode);
                if (variant == null) {
                    variant = new ProductVariant();
                    variant.setItemCode(itemCode);
                    variant.setProduct(parent);
                }
                variant.setStockQuantity(10); // Default or fetch from ERP
                variantRepo.save(variant);
                log.info("Catalog: Synced Variant {} for Parent {}", itemCode, variantOf);
                return; // Variants are not standalone products in this model likely
            }
        }

        // 3. Upsert Product
        boolean isNew = false;
        var product = productRepo.findByItemCode(itemCode);
        if (product == null) {
            product = new Product();
            product.setItemCode(itemCode);
            isNew = true;
        }

        // Preserve old state for event
        var oldPrice = product.getSpecialPrice();
        var oldQty = product.getQuantity();

        product.setProductName(name);
        product.setDescription(description != null && !description.isEmpty() ? description : name);
        product.setPrice(java.math.BigDecimal.valueOf(price));
        product.setSpecialPrice(java.math.BigDecimal.valueOf(price));
        product.setCategory(category);

        // Brand logic
        if (itemData.containsKey("brand")) {
            product.setBrand((String) itemData.get("brand"));
        }

        // Image logic
        if (imageUrl != null) {
            product.setImage(imageUrl);
        } else {
            if (product.getImage() == null)
                product.setImage("default.png");
        }

        product = productRepo.save(product);
        log.info("Catalog: Upserted Product: {} in Category: {}", itemCode, categoryName);

        // 4. Fetch and Sync Media (Files)
        syncMedia(product, itemCode);

        // 5. Publish Events
        if (isNew) {
            eventPublisher.publishEvent(new ProductCreatedEvent(
                    product.getProductId(),
                    product.getItemCode(),
                    product.getProductName(),
                    product.getSpecialPrice(),
                    product.getQuantity(),
                    product.getImage(),
                    product.getTags()));
        } else {
            eventPublisher.publishEvent(new ProductUpdatedEvent(
                    product.getProductId(),
                    product.getItemCode(),
                    oldPrice,
                    product.getSpecialPrice(),
                    oldQty,
                    product.getQuantity(),
                    product.getImage(),
                    product.getTags()));
        }
    }

    private void syncMedia(Product product, String itemCode) {
        try {
            var filesUrl = credentialProvider.getBaseUrl() + "/api/resource/File"
                    + "?filters=[[\"attached_to_name\",\"=\",\""
                    + itemCode + "\"]]&fields=[\"file_url\",\"is_private\",\"file_name\"]";

            RestClient restClient = restClientBuilder.build();
            ResponseEntity<Map<String, Object>> response = restClient.get()
                    .uri(filesUrl)
                    .header("Authorization",
                            "token " + credentialProvider.getApiKey() + ":" + credentialProvider.getApiSecret())
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (response.getBody().get("data") instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    var files = (List<Map<String, Object>>) list;

                    mediaRepo.deleteByProduct(product);

                    for (var file : files) {
                        var fileUrl = (String) file.get("file_url");
                        if (fileUrl == null)
                            continue;

                        var fullUrl = fileUrl.startsWith("http") ? fileUrl
                                : credentialProvider.getBaseUrl() + fileUrl;

                        var media = new ProductMedia();
                        media.setProduct(product);
                        media.setUrl(fullUrl);
                        media.setType("IMAGE");
                        // Note: BlurHash generation omitted to decouple from Discovery/ImageService

                        mediaRepo.save(media);
                    }
                    log.info("Catalog: Synced {} media items for Product: {}", files.size(), itemCode);
                }
            }
        } catch (Exception e) {
            log.warn("Catalog: Failed to sync media for item {}: {}", itemCode, e.getMessage());
        }
    }

    /**
     * Handle AI enrichment feedback from Discovery module.
     */
    @EventListener
    @Transactional
    public void onProductEnriched(ProductEnrichedEvent event) {
        log.info("Catalog: Received AI tags for product {}", event.itemCode());

        try {
            productRepo.findById(event.productId()).ifPresent(product -> {
                List<String> currentTags = product.getTags();
                if (currentTags == null) {
                    currentTags = new java.util.ArrayList<>();
                } else {
                    currentTags = new java.util.ArrayList<>(currentTags);
                }

                // Add new tags avoiding duplicates
                for (String tag : event.aiTags()) {
                    if (!currentTags.contains(tag)) {
                        currentTags.add(tag);
                    }
                }
                if (!currentTags.contains("AI-Enriched")) {
                    currentTags.add("AI-Enriched");
                }

                product.setTags(currentTags);
                productRepo.save(product);
                log.info("Catalog: Updated tags for product {}", event.itemCode());
            });
        } catch (Exception e) {
            log.error("Catalog: Failed to update tags for product {} - {}", event.itemCode(), e.getMessage());
        }
    }

    private Double getDouble(Object obj) {
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        return 0.0;
    }
}
