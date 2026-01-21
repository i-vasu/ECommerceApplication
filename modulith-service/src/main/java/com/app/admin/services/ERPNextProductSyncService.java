package com.app.admin.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.app.product.entities.Product;
import com.app.product.entities.ProductVariant;
import com.app.product.integration.SyncGateway;
import com.app.product.repositories.ProductRepo;
import com.app.product.repositories.ProductVariantRepo;
import com.app.core.multitenancy.Tenant;
import com.app.core.multitenancy.ERPNextCredentialProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ERPNextProductSyncService {

    private static final Logger log = LoggerFactory.getLogger(ERPNextProductSyncService.class);

    @Autowired
    private RestClient restClient;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ProductVariantRepo variantRepo;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ERPNextCredentialProvider credentialProvider;

    @Autowired
    private SyncGateway syncGateway;

    @Autowired
    private CacheManager cacheManager;

    public void syncItems(Tenant tenant) {
        log.info("Initiating Product Sync for Tenant: {} via Spring Integration Flow...", tenant.getTenantId());

        // Resolve effective credentials (fallback to global if tenant-specific are
        // missing or empty)
        String effectiveApiKey = (tenant.getErpNextApiKey() != null && !tenant.getErpNextApiKey().isBlank())
                ? tenant.getErpNextApiKey()
                : credentialProvider.getApiKey();
        String effectiveApiSecret = (tenant.getErpNextApiSecret() != null && !tenant.getErpNextApiSecret().isBlank())
                ? tenant.getErpNextApiSecret()
                : credentialProvider.getApiSecret();
        String effectiveUrl = tenant.getErpNextUrl();
        if (effectiveUrl == null || effectiveUrl.isBlank()) {
            effectiveUrl = credentialProvider.getBaseUrl();
        }
        if (effectiveUrl == null || effectiveUrl.isBlank()) {
            effectiveUrl = "http://localhost:8000";
        }
        effectiveUrl = effectiveUrl.trim();
        if (!effectiveUrl.startsWith("http")) {
            effectiveUrl = "http://" + effectiveUrl;
        }

        // Pass tenant info to integration flow via headers if needed,
        // but for now we'll update the global values temporarily or use a better way.
        // Given the singleton nature of the config, we'll pass it in the gateway call.
        syncGateway.startSync("MANUAL_TRIGGER_" + tenant.getTenantId(),
                effectiveUrl + "/api/resource/Item",
                effectiveApiKey,
                effectiveApiSecret);

        // Trigger Stock Sync
        syncStock(tenant);
    }

    public void syncStock(Tenant tenant) {
        String effectiveApiKey = tenant.getErpNextApiKey() != null ? tenant.getErpNextApiKey()
                : credentialProvider.getApiKey();
        String effectiveApiSecret = tenant.getErpNextApiSecret() != null ? tenant.getErpNextApiSecret()
                : credentialProvider.getApiSecret();
        String effectiveUrl = tenant.getErpNextUrl();
        if (effectiveUrl == null || effectiveUrl.isBlank()) {
            effectiveUrl = credentialProvider.getBaseUrl();
        }
        if (effectiveUrl == null || effectiveUrl.isBlank()) {
            effectiveUrl = "http://localhost:8000";
        }
        effectiveUrl = effectiveUrl.trim();
        if (!effectiveUrl.startsWith("http")) {
            effectiveUrl = "http://" + effectiveUrl;
        }

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

                    // Invalidate Cache to reflect stock updates
                    if (cacheManager.getCache("products") != null)
                        cacheManager.getCache("products").clear();
                    if (cacheManager.getCache("product") != null)
                        cacheManager.getCache("product").clear();
                }
            }
        } catch (Exception e) {
            log.error("Error syncing stock from ERPNext: {}", e.getMessage());
        }
    }

    // Single item sync (Read-Through)
    public int fetchStockFromERPNext(String itemCode) {
        log.info("Fetching real-time stock for item: {}", itemCode);
        try {
            // Fetch Bin data for the specific item
            // Using "actual_qty" from Bin doctype
            String url = credentialProvider.getBaseUrl() + "/api/resource/Bin"
                    + "?filters=[[\"item_code\",\"=\",\"" + itemCode + "\"]]&fields=[\"actual_qty\"]&limit=1";

            ResponseEntity<Map<String, Object>> response = restClient.get()
                    .uri(url)
                    .header("Authorization",
                            "token " + credentialProvider.getApiKey() + ":" + credentialProvider.getApiSecret())
                    .retrieve()
                    .toEntity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object dataObj = response.getBody().get("data");
                if (dataObj instanceof List<?> rawList && !rawList.isEmpty()) {
                    Map<?, ?> bin = (Map<?, ?>) rawList.get(0);
                    Double qty = getDouble(bin.get("actual_qty"));

                    // Update Cache while we have the fresh value
                    String redisKey = "inventory:stock:" + itemCode;
                    redisTemplate.opsForValue().set(redisKey, String.valueOf(qty.intValue()));

                    return qty.intValue();
                }
            }
            return 0; // Item likely has no stock entry yet
        } catch (Exception e) {
            log.error("Failed to fetch stock for {}: {}", itemCode, e.getMessage());
            // Fallback to Redis if API fails? Or return 0?
            // For now, fail safe 0.
            return 0;
        }
    }

    private Double getDouble(Object obj) {
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        return 0.0;
    }
}
