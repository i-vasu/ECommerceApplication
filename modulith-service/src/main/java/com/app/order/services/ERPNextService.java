package com.app.order.services;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.app.search.services.SearchService;
import com.app.product.payloads.ProductDTO;
import com.app.order.entites.Order;
import com.app.order.entites.OrderItem;
import com.app.identity.entities.User;

@Service
public class ERPNextService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ERPNextService.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("erpNextRestClient")
    private RestClient restClient;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.app.order.repositories.OrderRepo orderRepo;

    @Autowired(required = false)
    private SearchService searchService;

    @Value("${erpnext.api.base-url:http://localhost:8000}/api/resource/Item")
    private String erpNextUrl;

    @Value("${erpnext.api.key:}")
    private String apiKey;

    @Value("${erpnext.api.secret:}")
    private String apiSecret;

    private String getAuthHeader() {
        return "token " + apiKey + ":" + apiSecret;
    }

    public void createCustomer(User user) {
        if (apiKey == null || apiKey.isEmpty())
            return;
        try {
            String customerUrl = erpNextUrl.replace("Item", "Customer");
            Map<String, Object> customer = new HashMap<>();
            customer.put("customer_name", user.getFirstName() + " " + user.getLastName());
            customer.put("customer_type", "Individual");
            customer.put("email_id", user.getEmail());
            customer.put("mobile_no", user.getMobileNumber());

            restClient.post()
                    .uri(customerUrl)
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(customer)
                    .retrieve()
                    .toBodilessEntity();
            System.out.println(">>> Customer created in ERPNext for: " + user.getEmail());
        } catch (Exception e) {
            System.err.println(">>> Error creating customer in ERPNext: " + e.getMessage());
        }
    }

    public boolean checkStock(String itemCode, Integer quantity) {
        if (apiKey == null || apiKey.isEmpty() || itemCode == null)
            return true;
        try {
            String binUrl = erpNextUrl.replace("resource/Item", "method/frappe.client.get_value");
            String url = binUrl + "?doctype=Bin&filters={\"item_code\":\"" + itemCode + "\"}&fieldname=actual_qty";

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", getAuthHeader())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            double actualQty = root.has("message") && root.get("message").has("actual_qty")
                    ? root.get("message").get("actual_qty").asDouble()
                    : 0.0;

            return actualQty >= quantity;
        } catch (Exception e) {
            System.err.println(">>> Error checking stock in ERPNext: " + e.getMessage());
            return true;
        }
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "erpnext", fallbackMethod = "createSalesOrderFallback")
    @io.github.resilience4j.retry.annotation.Retry(name = "erpnext")
    @org.springframework.scheduling.annotation.Async
    public void createSalesOrderAsync(Order order) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("ERPNext API key not configured. Skipping sales order creation for Order: {}", order.getOrderId());
            return;
        }

        org.slf4j.MDC.put("orderId", String.valueOf(order.getOrderId()));
        log.info("Creating Sales Order in ERPNext for Order: {}", order.getOrderId());

        try {
            // First, ensure Customer exists
            createCustomerIfNotExists(order);

            Map<String, Object> salesOrder = new HashMap<>();
            salesOrder.put("doctype", "Sales Order");
            salesOrder.put("customer", order.getEmail()); // Using email as customer name/ID
            salesOrder.put("transaction_date", java.time.LocalDate.now().toString());
            salesOrder.put("delivery_date", java.time.LocalDate.now().plusDays(7).toString());
            salesOrder.put("po_no", String.valueOf(order.getOrderId())); // Reference to our Order ID

            List<Map<String, Object>> items = new ArrayList<>();
            for (OrderItem orderItem : order.getOrderItems()) {
                Map<String, Object> item = new HashMap<>();
                item.put("item_code",
                        orderItem.getItemCode() != null ? orderItem.getItemCode() : orderItem.getProductName());
                item.put("qty", orderItem.getQuantity());
                item.put("rate", orderItem.getOrderedPrice());
                items.add(item);
            }
            salesOrder.put("items", items);
            salesOrder.put("docstatus", 1); // Submit the document immediately

            String salesOrderUrl = erpNextUrl.replace("Item", "Sales Order");
            JsonNode response = restClient.post()
                    .uri(salesOrderUrl)
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(salesOrder)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("data") && response.get("data").has("name")) {
                String erpName = response.get("data").get("name").asText();
                order.setErpNextOrderName(erpName);
                orderRepo.save(order);
                log.info("Sales Order created successfully in ERPNext. Order: {} -> ERPNext: {}", order.getOrderId(),
                        erpName);
            }
        } catch (Exception e) {
            log.error("Failed to create Sales Order in ERPNext for Order: {}", order.getOrderId(), e);
            throw e; // Re-throw to trigger circuit breaker
        } finally {
            org.slf4j.MDC.remove("orderId");
        }
    }

    private void createCustomerIfNotExists(Order order) {
        try {
            // Check if customer exists
            String customerUrl = erpNextUrl.replace("Item", "Customer") + "/" + order.getEmail();
            try {
                restClient.get().uri(customerUrl).header("Authorization", getAuthHeader()).retrieve()
                        .toBodilessEntity();
                return; // Customer exists
            } catch (Exception e) {
                // Customer likely doesn't exist, proceed to create
            }

            Map<String, Object> customer = new HashMap<>();
            customer.put("customer_name", order.getEmail()); // Use email as name if real name unavailable
            customer.put("customer_type", "Individual");
            customer.put("customer_group", "All Customer Groups");
            customer.put("territory", "All Territories");
            customer.put("email_id", order.getEmail());

            String createUrl = erpNextUrl.replace("Item", "Customer");
            restClient.post()
                    .uri(createUrl)
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(customer)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Created new Customer in ERPNext: {}", order.getEmail());
        } catch (Exception e) {
            log.warn("Failed to create customer in ERPNext (might already exist or API error): {}", e.getMessage());
        }
    }

    // Fallback method when ERPNext is unavailable
    private void createSalesOrderFallback(Order order, Exception e) {
        log.error("ERPNext circuit breaker activated. Fallback triggered for Order: {}. Reason: {}",
                order.getOrderId(), e.getMessage());
        // In production, you might:
        // 1. Publish to DLQ for manual intervention
        // 2. Store in a "pending sync" table
        // 3. Send alert to operations team
    }

    public void syncProductsFromERPNext() {
        if (apiKey == null || apiKey.isEmpty())
            return;
        try {
            String url = erpNextUrl
                    + "?fields=[\"item_code\",\"item_name\",\"description\",\"standard_rate\",\"image\"]";
            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", getAuthHeader())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");
            List<ProductDTO> products = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode node : data) {
                    ProductDTO p = new ProductDTO();
                    p.setItemCode(node.get("item_code").asText());
                    p.setProductName(node.get("item_name").asText());
                    p.setDescription(node.has("description") ? node.get("description").asText() : "");
                    p.setPrice(node.has("standard_rate") ? node.get("standard_rate").asDouble() : 0.0);
                    p.setImage(node.has("image") ? node.get("image").asText() : "default.png");
                    // Assign a stable Long ID based on hash if needed for DTO requirements
                    p.setProductId((long) node.get("item_code").asText().hashCode());
                    products.add(p);
                }
            }
            if (searchService != null) {
                searchService.indexProducts(products);
            }
            System.out.println(">>> Synced " + products.size() + " items from ERPNext.");
        } catch (Exception e) {
            System.err.println(">>> Error syncing products: " + e.getMessage());
        }
    }

    public void updateOrderStatuses() {
        if (apiKey == null || apiKey.isEmpty())
            return;

        List<Order> pendingOrders = orderRepo.findAll().stream()
                .filter(o -> o.getErpNextOrderName() != null)
                .filter(o -> !"DELIVERED".equalsIgnoreCase(o.getOrderStatus())
                        && !"CANCELLED".equalsIgnoreCase(o.getOrderStatus()))
                .toList();

        for (Order order : pendingOrders) {
            try {
                String erpSalesOrderUrl = erpNextUrl.replace("Item", "Sales Order");
                String url = erpSalesOrderUrl + "/" + order.getErpNextOrderName();
                JsonNode response = restClient.get()
                        .uri(url)
                        .header("Authorization", getAuthHeader())
                        .retrieve()
                        .body(JsonNode.class);

                if (response != null && response.has("data")) {
                    String erpStatus = response.get("data").get("status").asText();
                    String localStatus = mapErpStatus(erpStatus);

                    if (!localStatus.equalsIgnoreCase(order.getOrderStatus())) {
                        order.setOrderStatus(localStatus);

                        // Capture Tracking Info if Shipped/Delivered
                        // ERPNext Delivery Note usually holds this, or custom fields in Sales Order
                        // For simplicity, we check if "tracking_number" exists in Sales Order custom
                        // field
                        if (response.get("data").has("tracking_number")) {
                            // Logic to create/update Shipment entity
                            updateShipmentInfo(order, response.get("data"));
                        }

                        orderRepo.save(order);
                        System.out.println(">>> Order " + order.getOrderId() + " status updated to " + localStatus);
                    }
                }
            } catch (Exception e) {
                System.err
                        .println(">>> Error fetching status for order " + order.getOrderId() + ": " + e.getMessage());
            }
        }
    }

    public void cancelSalesOrder(String erpOrderName) {
        if (apiKey == null || apiKey.isEmpty())
            return;
        try {
            String url = erpNextUrl.replace("Item", "Sales Order") + "/" + erpOrderName;
            Map<String, Object> body = new HashMap<>();
            body.put("status", "Cancelled");

            restClient.put()
                    .uri(url)
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            System.out.println(">>> Order " + erpOrderName + " cancelled in ERPNext.");
        } catch (Exception e) {
            System.err.println(">>> Error cancelling Sales Order in ERPNext: " + e.getMessage());
        }
    }

    @Autowired
    private com.app.order.repositories.ShipmentRepo shipmentRepo;

    private void updateShipmentInfo(Order order, JsonNode salesOrderData) {
        try {
            com.app.order.entites.Shipment shipment = order.getShipment();
            if (shipment == null) {
                shipment = new com.app.order.entites.Shipment();
                shipment.setOrder(order);
                shipment.setCarrier("ERPNext"); // Default carrier if from ERP
                shipment.setStatus("SHIPPED");
                order.setShipment(shipment);
            }

            if (salesOrderData.has("tracking_number")) {
                shipment.setAwbNumber(salesOrderData.get("tracking_number").asText());
            }
            if (salesOrderData.has("courier_name")) {
                shipment.setCourierName(salesOrderData.get("courier_name").asText());
            }

            // If we have specific shipment ID from ERP
            if (salesOrderData.has("delivery_note")) {
                shipment.setExternalShipmentId(salesOrderData.get("delivery_note").asText());
            }

            shipmentRepo.save(shipment);
            log.info("Updated Shipment info for Order {} from ERPNext", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to update shipment info from ERPNext: {}", e.getMessage());
        }
    }

    private String mapErpStatus(String erpStatus) {
        return switch (erpStatus.toUpperCase()) {
            case "COMPLETED" -> "DELIVERED";
            case "CANCELLED" -> "CANCELLED";
            case "DRAFT" -> "PENDING";
            case "ON HOLD" -> "HOLD";
            default -> "PROCESSING";
        };
    }
}
