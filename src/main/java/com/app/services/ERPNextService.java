package com.app.services;

import com.app.entites.Category;
import com.app.entites.Product;
import com.app.repositories.CategoryRepo;
import com.app.repositories.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class ERPNextService {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${erpnext.api.url:http://localhost:8000/api/resource/Item}")
    private String erpNextUrl;

    @Value("${erpnext.api.key:}")
    private String apiKey;

    @Value("${erpnext.api.secret:}")
    private String apiSecret;

    @Scheduled(fixedRate = 60000) // Poll every 60 seconds
    public void syncItems() {
        System.out.println(">>> Starting ERPNext sync...");

        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            System.out.println(">>> ERPNext API keys are missing. Skipping sync.");
            return;
        }

        try {
            System.out.println(">>> Connecting to ERPNext at: " + erpNextUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(erpNextUrl
                    + "?fields=[\"name\",\"item_name\",\"description\",\"standard_rate\",\"item_group\",\"image\"]",
                    HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("data");
                System.out.println(">>> Found " + (items != null ? items.size() : 0) + " items in ERPNext.");
                if (items != null) {
                    for (Map<String, Object> item : items) {
                        saveOrUpdateProduct(item);
                    }
                }
            } else {
                System.out.println(">>> Failed to fetch items. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println(">>> Error syncing with ERPNext: " + e.getMessage());
        }
    }

    private void saveOrUpdateProduct(Map<String, Object> itemData) {
        String itemName = (String) itemData.get("item_name");
        System.out.println(">>> Syncing product: " + itemName);

        String description = (String) itemData.get("description");
        if (description == null || description.isEmpty())
            description = itemName;

        Double price = 0.0;
        Object rate = itemData.get("standard_rate");
        if (rate instanceof Number) {
            price = ((Number) rate).doubleValue();
        }

        String categoryName = (String) itemData.get("item_group");
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
        product.setPrice(price);

        // Fix: Set specialPrice to ensure frontend displays the cost
        product.setSpecialPrice(price);
        product.setDiscount(0.0);

        product.setCategory(category);
        product.setQuantity(100); // Default quantity
        product.setImage((String) itemData.get("image"));
        if (product.getImage() == null)
            product.setImage("default.png");

        productRepo.save(product);
    }
}
