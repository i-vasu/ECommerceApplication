package com.app.admin.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.app.product.entites.Product;
import com.app.product.entites.ProductVariant;
import com.app.product.integration.SyncGateway;
import com.app.product.repositories.ProductRepo;
import com.app.product.repositories.ProductVariantRepo;

import lombok.extern.slf4j.Slf4j;

@Service
public class ERPNextProductSyncService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ERPNextProductSyncService.class);

    @Autowired
    private RestClient restClient;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ProductVariantRepo variantRepo;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.app.core.multitenancy.ERPNextCredentialProvider credentialProvider;

    @Autowired
    private SyncGateway syncGateway;

    public void syncItems(com.app.core.multitenancy.Tenant tenant) {
        log.info("Initiating Product Sync for Tenant: {} via Spring Integration Flow...", tenant.getTenantId());

        // Pass tenant info to integration flow via headers if needed,
        // but for now we'll update the global values temporarily or use a better way.
        // Given the singleton nature of the config, we'll pass it in the gateway call.
        syncGateway.startSync("MANUAL_TRIGGER_" + tenant.getTenantId(),
                tenant.getErpNextUrl() + "/api/resource/Item",
                tenant.getErpNextApiKey(),
                tenant.getErpNextApiSecret());

        // Trigger Stock Sync
        syncStock(tenant);
    }

    public void syncStock(com.app.core.multitenancy.Tenant tenant) {
        String effectiveApiKey = tenant.getErpNextApiKey() != null ? tenant.getErpNextApiKey()
                : credentialProvider.getApiKey();
        String effectiveApiSecret = tenant.getErpNextApiSecret() != null ? tenant.getErpNextApiSecret()
                : credentialProvider.getApiSecret();
        String effectiveUrl = tenant.getErpNextUrl() != null ? tenant.getErpNextUrl() : credentialProvider.getBaseUrl();

        if (effectiveApiKey == null || effectiveApiKey.isEmpty())
            return;
        try {
            // Fetch Bin (Stock) data
            String url = effectiveUrl + "/api/resource/Bin"
                    + "?fields=[\"item_code\",\"actual_qty\"]&limit_page_length=2000";

            ResponseEntity<Map<String, Object>> response = restClient.get()
                    .uri(url)
                    .header("Authorization", "token " + effectiveApiKey + ":" + effectiveApiSecret)
                    .retrieve()
                    .toEntity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object dataObj = response.getBody().get("data");
                if (dataObj instanceof List<?> rawList) {
                    for (Object item : rawList) {
                        if (item instanceof Map<?, ?> bin) {
                            String itemCode = (String) bin.get("item_code");
                            Double qty = getDouble(bin.get("actual_qty"));

                            // 1. Update Redis Cache (DragonflyDB) for Reservation Service
                            // Key format from InventoryReservationService: inventory:stock:ITEM_CODE
                            String redisKey = "inventory:stock:" + itemCode;
                            redisTemplate.opsForValue().set(redisKey, String.valueOf(qty.intValue()));

                            // 2. Update Postgres (ProductVariant)
                            // This might be slow for 2000 items, ideally batch or async
                            // For now we do it directly for simplicity/correctness
                            ProductVariant variant = variantRepo.findByItemCode(itemCode);
                            if (variant != null) {
                                variant.setStockQuantity(qty.intValue());
                                variantRepo.save(variant);
                            }
                        }
                    }
                    log.info("Synced stock levels for {} items from ERPNext", rawList.size());
                }
            }
        } catch (Exception e) {
            log.error("Error syncing stock from ERPNext: {}", e.getMessage());
        }
    }

    private void saveOrUpdateVariant(Map<String, Object> itemData, String parentItemCode) {
        String itemCode = (String) itemData.get("name");
        Product parent = productRepo.findByItemCode(parentItemCode);

        if (parent == null) {
            log.warn("Parent product {} not found for variant {}. Skipping.", parentItemCode, itemCode);
            return;
        }

        ProductVariant variant = variantRepo.findByItemCode(itemCode);
        if (variant == null) {
            variant = new ProductVariant();
            variant.setItemCode(itemCode);
            variant.setProduct(parent);
        }

        // In a real scenario, you'd fetch attributes like Color, Size from another API
        // or fields
        // For now, we'll try to infer or set defaults
        variant.setStockQuantity(100);

        // Sync Attributes
        if (itemData.containsKey("material")) {
            variant.setMaterial((String) itemData.get("material"));
        }

        // Extract size/color from itemName if possible (e.g. "T-Shirt - Red - XL")
        String itemName = (String) itemData.get("item_name");
        if (itemName != null && itemName.contains("-")) {
            String[] parts = itemName.split("-");
            if (parts.length >= 2)
                variant.setColor(parts[1].trim());
            if (parts.length >= 3)
                variant.setSize(parts[2].trim());
        }

        variantRepo.save(variant);
        log.info("Synced variant: {} for parent: {}", itemCode, parentItemCode);
    }

    private String formatImageUrl(String image) {
        if (image != null && !image.startsWith("http")) {
            // Use dynamic base URL
            return credentialProvider.getBaseUrl() + image;
        }
        return image;
    }

    private Double getDouble(Object obj) {
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        return 0.0;
    }
}
