package com.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ERPNextService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MinioService minioService;

    @Value("${erpnext.api.url:http://localhost:8000/api/resource/Item}")
    private String erpNextUrl;

    @Value("${erpnext.api.key:}")
    private String apiKey;

    @Value("${erpnext.api.secret:}")
    private String apiSecret;

    public void createSalesOrder(com.app.entites.Order order) {
        System.out.println(">>> Syncing Order to ERPNext: " + order.getOrderId());
        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            System.err.println(">>> ERPNext API keys missing. Skipping Order Sync.");
            return;
        }

        try {
            Map<String, Object> salesOrder = new java.util.HashMap<>();
            salesOrder.put("doctype", "Sales Order");
            salesOrder.put("customer", "Administrator"); // Default for now, or fetch
            salesOrder.put("transaction_date", java.time.LocalDate.now().toString());
            salesOrder.put("delivery_date", java.time.LocalDate.now().plusDays(7).toString());

            java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (com.app.entites.OrderItem orderItem : order.getOrderItems()) {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("item_code", orderItem.getProductName()); // Assuming Name is Item Code
                item.put("qty", orderItem.getQuantity());
                item.put("rate", orderItem.getOrderedProductPrice());
                items.add(item);
            }
            salesOrder.put("items", items);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(salesOrder, headers);

            // ERPNext resource URL for Sales Order
            String salesOrderUrl = erpNextUrl.replace("Item", "Sales Order");

            ResponseEntity<String> response = restTemplate.postForEntity(salesOrderUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println(">>> Order synced successfully to ERPNext!");
            } else {
                System.err
                        .println(">>> Failed to sync order: " + response.getStatusCode() + " - " + response.getBody());
            }

        } catch (Exception e) {
            System.err.println(">>> Error syncing order to ERPNext: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
