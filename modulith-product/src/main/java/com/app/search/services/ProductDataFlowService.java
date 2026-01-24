package com.app.search.services;

import com.app.product.entities.Category;
import com.app.product.entities.Product;
import com.app.product.entities.ProductVariant;
import com.app.product.repositories.CategoryRepo;
import com.app.product.repositories.ProductRepo;
import com.app.product.repositories.ProductVariantRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.io.InputStream;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import com.app.product.repositories.ProductMediaRepo;
import com.app.core.multitenancy.ERPNextCredentialProvider;
import com.app.product.entities.ProductMedia;
import java.util.concurrent.CompletableFuture;

import com.app.media.ImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Service
public class ProductDataFlowService {

    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final ProductVariantRepo variantRepo;
    private final ProductMediaRepo mediaRepo;
    private final ImageService imageService;
    private final VisualSearchService visualSearchService;
    private final RestClient restClient;
    private final ERPNextCredentialProvider credentialProvider;

    @Transactional
    public void upsertProductFromERPNext(String itemCode, String name, String description, Double price,
            String imageUrl, String categoryName) {

        var category = categoryRepo.findByCategoryName(categoryName);
        if (category == null) {
            category = new Category();
            category.setCategoryName(categoryName);
            category = categoryRepo.save(category);
        }

        var product = productRepo.findByItemCode(itemCode);
        if (product == null) {
            product = new Product();
            product.setItemCode(itemCode);
        }

        product.setProductName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setSpecialPrice(price);
        product.setCategory(category);
        if (imageUrl != null)
            product.setImage(imageUrl);

        productRepo.save(product);
    }

    @Transactional
    public void processItemWithMedia(Map<String, Object> itemData) {
        var itemCode = (String) itemData.get("name");
        var name = (String) itemData.get("item_name");
        var description = (String) itemData.get("description");
        var price = getDouble(itemData.get("standard_rate"));
        var imageUrl = (String) itemData.get("image");
        var categoryName = (String) itemData.get("item_group");
        var variantOf = (String) itemData.get("variant_of");

        if (categoryName == null) {
            categoryName = "Default";
        }

        var category = categoryRepo.findByCategoryName(categoryName);
        if (category == null) {
            category = new Category();
            category.setCategoryName(categoryName);
            category = categoryRepo.save(category);
            log.info("Created new Category: {}", categoryName);
        }

        if (variantOf != null && !variantOf.isEmpty()) {
            var parent = productRepo.findByItemCode(variantOf);
            if (parent != null) {
                var variant = variantRepo.findByItemCode(itemCode);
                if (variant == null) {
                    variant = new ProductVariant();
                    variant.setItemCode(itemCode);
                    variant.setProduct(parent);
                }
                variant.setStockQuantity(10);
                variantRepo.save(variant);
                log.info("Synced Variant {} for Parent {}", itemCode, variantOf);
                return;
            }
        }

        var product = productRepo.findByItemCode(itemCode);
        if (product == null) {
            product = new Product();
            product.setItemCode(itemCode);
        }

        product.setProductName(name);
        product.setDescription(description != null && !description.isEmpty() ? description : name);
        product.setPrice(price);
        product.setSpecialPrice(price);
        product.setCategory(category);

        if (itemData.containsKey("brand")) {
            product.setBrand((String) itemData.get("brand"));
        }

        if (imageUrl != null) {
            product.setImage(imageUrl);
        } else {
            if (product.getImage() == null)
                product.setImage("default.png");
        }

        product = productRepo.save(product);
        log.info("Upserted Product: {} in Category: {}", itemCode, categoryName);

        try {
            var filesUrl = credentialProvider.getBaseUrl() + "/api/resource/File"
                    + "?filters=[[\"attached_to_name\",\"=\",\""
                    + itemCode + "\"]]&fields=[\"file_url\",\"is_private\",\"file_name\"]";

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

                        try {
                            if (fullUrl != null) {
                                try (InputStream is = restClient.get()
                                        .uri(fullUrl)
                                        .retrieve()
                                        .body(InputStream.class)) {

                                    if (is != null) {
                                        var hashFuture = imageService.generateBlurHash(is);
                                        var hash = hashFuture.join();
                                        if (hash != null) {
                                            media.setBlurHash(hash);
                                        }
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

            var currentImage = product.getImage();
            if (currentImage != null && !currentImage.isEmpty()) {
                try {
                    var fullImageUrl = currentImage.startsWith("http") ? currentImage
                            : credentialProvider.getBaseUrl() + currentImage;

                    visualSearchService.updateProductVector(product.getProductId(), fullImageUrl);

                } catch (Exception e) {
                    log.warn("Visual Embedding failed for {}: {}", itemCode, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error syncing media for {}: {}", itemCode, e.getMessage());
        }
    }

    private Double getDouble(Object obj) {
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        return 0.0;
    }
}
