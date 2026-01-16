package com.app.inventory;

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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.app.entites.Product;
import com.app.entites.ProductVariant;

import com.app.repositories.ProductRepo;
import com.app.repositories.ProductVariantRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ERPNextProductSyncService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ProductVariantRepo variantRepo;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${erpnext.api.url:http://localhost:8000/api/resource/Item}")
    private String erpNextUrl;

    @Value("${erpnext.api.key:}")
    private String apiKey;

    @Value("${erpnext.api.secret:}")
    private String apiSecret;

    @Autowired
    private com.app.integration.SyncGateway syncGateway;

    public void syncItems() {
        log.info("Initiating Product Sync via Spring Integration Flow...");
        syncGateway.startSync("MANUAL_TRIGGER");

        // Trigger Stock Sync concurrently or after
        // Ideally we listen to "SYNC_COMPLETE" event, but for now we just trigger it.
        // Since syncItems() is likely async or fast now.
        syncStock();
    }

    public void syncStock() {
        if (apiKey == null)
            return;
        try {
            // Fetch Bin (Stock) data
            // Fields: item_code, actual_qty
            String url = erpNextUrl.replace("Item", "Bin")
                    + "?fields=[\"item_code\",\"actual_qty\"]&limit_page_length=2000";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> bins = (List<Map<String, Object>>) response.getBody().get("data");
                if (bins != null) {
                    for (Map<String, Object> bin : bins) {
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
                    log.info("Synced stock levels for {} items from ERPNext", bins.size());
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
            return erpNextUrl.split("/api")[0] + image;
        }
        return image;
    }

    private Double getDouble(Object obj) {
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        return 0.0;
    }
}
