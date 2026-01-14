package com.app.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.app.entites.Category;
import com.app.entites.Product;
import com.app.repositories.CategoryRepo;
import com.app.repositories.ProductRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ERPNextProductSyncService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    @Value("${erpnext.api.url:http://localhost:8000/api/resource/Item}")
    private String erpNextUrl;

    @Value("${erpnext.api.key:}")
    private String apiKey;

    @Value("${erpnext.api.secret:}")
    private String apiSecret;

    public void syncItems() {
        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            log.warn("ERPNext API keys missing. Skipping Product Sync.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Fetch list of items
            String url = erpNextUrl
                    + "?fields=[\"name\",\"item_name\",\"description\",\"standard_rate\",\"image\",\"item_group\"]";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("data");
                if (items != null) {
                    for (Map<String, Object> itemData : items) {
                        saveOrUpdateProduct(itemData);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error syncing products from ERPNext: {}", e.getMessage());
        }
    }

    private void saveOrUpdateProduct(Map<String, Object> itemData) {
        String itemName = (String) itemData.get("item_name");
        String itemCode = (String) itemData.get("name");
        String description = (String) itemData.get("description");
        Double rate = (Double) itemData.get("standard_rate");
        String image = (String) itemData.get("image");
        String categoryName = (String) itemData.get("item_group");

        if (itemName == null)
            itemName = itemCode;
        if (description == null)
            description = "No description provided.";
        if (rate == null)
            rate = 0.0;

        Category category = categoryRepo.findByCategoryName(categoryName);
        if (category == null) {
            category = new Category();
            category.setCategoryName(categoryName);
            category = categoryRepo.save(category);
        }

        Product product = productRepo.findByProductName(itemName);
        if (product == null) {
            product = new Product();
            product.setProductName(itemName);
        }

        product.setDescription(description);
        product.setPrice(rate);
        product.setSpecialPrice(rate); // Default
        product.setCategory(category);
        product.setQuantity(100); // Default stock if not fetched

        if (image != null && !image.isEmpty()) {
            if (!image.startsWith("http")) {
                image = erpNextUrl.split("/api")[0] + image;
            }
            product.setImage(image);
        }

        productRepo.save(product);
        log.info("Synced product: {}", itemName);
    }
}
