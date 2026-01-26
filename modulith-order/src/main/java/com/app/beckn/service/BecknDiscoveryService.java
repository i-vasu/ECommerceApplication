package com.app.beckn.service;

import com.app.product.entities.Product;
import com.app.product.repositories.ProductRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BecknDiscoveryService {

    private final ProductRepo productRepo;

    /**
     * Map Beckn search intent to ParadeDB queries
     */
    public Map<String, Object> processSearch(Map<String, Object> becknRequest) {
        log.info("Processing Beckn Search Intent...");
        
        String keyword = "fashion";
        Double latitude = null;
        Double longitude = null;

        try {
            Map<?, ?> message = (Map<?, ?>) becknRequest.get("message");
            Map<?, ?> intent = (Map<?, ?>) message.get("intent");
            
            // 1. Keyword Extraction
            if (intent.containsKey("item")) {
                Map<?, ?> item = (Map<?, ?>) intent.get("item");
                Map<?, ?> descriptor = (Map<?, ?>) item.get("descriptor");
                keyword = (String) descriptor.get("name");
            }

            // 2. Hyper-Local Context (Strategic Capability)
            if (intent.containsKey("fulfillment")) {
                Map<?, ?> fulfillment = (Map<?, ?>) intent.get("fulfillment");
                Map<?, ?> end = (Map<?, ?>) fulfillment.get("end");
                Map<?, ?> location = (Map<?, ?>) end.get("location");
                String gps = (String) location.get("gps");
                if (gps != null) {
                    String[] parts = gps.split(",");
                    latitude = Double.parseDouble(parts[0]);
                    longitude = Double.parseDouble(parts[1]);
                }
            }
        } catch (Exception e) {
            log.warn("Search metadata extraction failed, using defaults. Error: {}", e.getMessage());
        }

        // 3. AI-Driven Matching (Leveraging AI Tags from ProductDataFlowService)
        // We query ParadeDB using BM25, which naturally searches across Name, Description, and our AI-Tags
        Page<Product> results = productRepo.searchByKeyword(keyword, PageRequest.of(0, 10));
        
        // 4. City-Level/Local Filtering Logic (Simulated)
        if (latitude != null) {
            log.info("Applying geo-spatial filter for: {},{}", latitude, longitude);
            // In production, we'd use ParadeDB/PostGIS: ST_Distance(location, ST_MakePoint(lng, lat)) < 10000
        }

        return buildBecknCatalog(results.getContent());
    }

    private Map<String, Object> buildBecknCatalog(List<Product> products) {
        Map<String, Object> catalog = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        for (Product product : products) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getItemCode());
            item.put("descriptor", Map.of(
                    "name", product.getProductName(),
                    "long_desc", product.getDescription(),
                    "images", List.of(product.getImage())
            ));
            item.put("price", Map.of(
                    "currency", "INR",
                    "value", product.getPrice().toString()
            ));
            items.add(item);
        }

        Map<String, Object> bppProvider = new HashMap<>();
        bppProvider.put("id", "fashion-store-bpp");
        bppProvider.put("descriptor", Map.of("name", "Unified Fashion Network"));
        bppProvider.put("items", items);

        catalog.put("bpp/descriptor", Map.of("name", "Unified Fashion Network"));
        catalog.put("bpp/providers", List.of(bppProvider));

        return Map.of("catalog", catalog);
    }
}
