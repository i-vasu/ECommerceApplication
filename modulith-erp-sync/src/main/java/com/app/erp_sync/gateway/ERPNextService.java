package com.app.erp_sync.gateway;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
// import com.app.search.services.SearchService;
import com.app.catalog.payloads.ProductDTO;
import com.app.order.entities.Order;
import com.app.order.entities.OrderItem;
import com.app.logistics.entities.Shipment;
import com.app.security.entities.User;
import com.app.order.repositories.OrderRepo;
import com.app.logistics.repositories.ShipmentRepo;
import com.app.core.async.EventProducer;
import com.app.core.events.OrderStatusEvent;
import com.app.governance.states.OrderStatus;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.MDC;
import com.app.core.multitenancy.ERPNextCredentialProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Service
public class ERPNextService {

    @Qualifier("erpNextRestClient")
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OrderRepo orderRepo;
    private final ShipmentRepo shipmentRepo;
    // private final SearchService searchService;
    private final EventProducer eventProducer;
    private final ERPNextCredentialProvider credentialProvider;
    private final StringRedisTemplate redisTemplate;
    private final com.app.core.services.RedisLockService lockService;

    private String getErpNextUrl() {
        return credentialProvider.getBaseUrl();
    }

    private String getApiKey() {
        return credentialProvider.getApiKey();
    }

    private String getApiSecret() {
        return credentialProvider.getApiSecret();
    }

    private String getAuthHeader() {
        return "token " + getApiKey() + ":" + getApiSecret();
    }

    public void createCustomer(User user) {
        if (getApiKey() == null || getApiKey().isEmpty())
            return;
        try {
            var customerUrl = getErpNextUrl() + "/api/resource/Customer";
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
            log.info("Customer created in ERPNext for: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error creating customer in ERPNext: {}", e.getMessage());
        }
    }

    @org.springframework.context.event.EventListener
    public void onUserRegistered(com.app.core.events.UserRegisteredEvent event) {
        User user = new User();
        user.setEmail(event.email());
        user.setFirstName(event.firstName());
        user.setLastName(event.lastName());
        createCustomer(user);
    }

    public boolean checkStock(String itemCode, Integer quantity) {
        if (getApiKey() == null || getApiKey().isEmpty() || itemCode == null)
            return true;
        try {
            var binUrl = getErpNextUrl() + "/api/method/frappe.client.get_value";
            var url = binUrl + "?doctype=Bin&filters={\"item_code\":\"" + itemCode + "\"}&fieldname=actual_qty";

            var response = restClient.get()
                    .uri(url)
                    .header("Authorization", getAuthHeader())
                    .retrieve()
                    .body(String.class);

            var root = objectMapper.readTree(response);
            var actualQty = root.has("message") && root.get("message").has("actual_qty")
                    ? root.get("message").get("actual_qty").asDouble()
                    : 0.0;

            return actualQty >= quantity;
        } catch (Exception e) {
            log.error("Error checking stock in ERPNext: {}", e.getMessage());
            return true;
        }
    }

    public void createSalesOrderByOrderId(Long orderId) {
        orderRepo.findById(orderId).ifPresent(this::createSalesOrderAsync);
    }

    @CircuitBreaker(name = "erpnext", fallbackMethod = "createSalesOrderFallback")
    @Retry(name = "erpnext")
    @Async
    public void createSalesOrderAsync(Order order) {
        if (getApiKey() == null || getApiKey().isEmpty()) {
            log.warn("ERPNext API key not configured. Skipping sales order creation for Order: {}", order.getOrderId());
            return;
        }

        MDC.put("orderId", String.valueOf(order.getOrderId()));
        log.info("Creating Sales Order in ERPNext for Order: {}", order.getOrderId());

        try {
            createCustomerIfNotExists(order);

            Map<String, Object> salesOrder = new HashMap<>();
            salesOrder.put("doctype", "Sales Order");
            salesOrder.put("customer", order.getEmail());
            salesOrder.put("transaction_date", LocalDate.now().toString());
            salesOrder.put("delivery_date", LocalDate.now().plusDays(7).toString());
            salesOrder.put("po_no", String.valueOf(order.getOrderId()));
            salesOrder.put("company", credentialProvider.getCompanyName());
            salesOrder.put("set_warehouse", credentialProvider.getWarehouse());

            List<Map<String, Object>> items = new ArrayList<>();
            for (var orderItem : order.getOrderItems()) {
                Map<String, Object> item = new HashMap<>();
                item.put("item_code",
                        orderItem.getItemCode() != null ? orderItem.getItemCode() : orderItem.getProductName());
                item.put("qty", orderItem.getQuantity());
                item.put("rate", orderItem.getOrderedPrice());
                items.add(item);
            }
            salesOrder.put("items", items);
            salesOrder.put("docstatus", 1);

            var salesOrderUrl = getErpNextUrl() + "/api/resource/Sales Order";
            var response = restClient.post()
                    .uri(salesOrderUrl)
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(salesOrder)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("data") && response.get("data").has("name")) {
                var erpName = response.get("data").get("name").asText();
                order.setErpNextOrderName(erpName);
                orderRepo.save(order);
                log.info("Sales Order created successfully in ERPNext. Order: {} -> ERPNext: {}", order.getOrderId(),
                        erpName);
            }
        } catch (Exception e) {
            log.error("Failed to create Sales Order in ERPNext for Order: {}", order.getOrderId(), e);
            throw e;
        } finally {
            MDC.remove("orderId");
        }
    }

    private void createCustomerIfNotExists(Order order) {
        try {
            var customerUrl = getErpNextUrl() + "/api/resource/Customer/" + order.getEmail();
            try {
                restClient.get().uri(customerUrl).header("Authorization", getAuthHeader()).retrieve()
                        .toBodilessEntity();
                return;
            } catch (Exception e) {
                // Ignore
            }

            Map<String, Object> customer = new HashMap<>();
            customer.put("customer_name", order.getEmail());
            customer.put("customer_type", "Individual");
            customer.put("customer_group", "All Customer Groups");
            customer.put("territory", "All Territories");
            customer.put("email_id", order.getEmail());

            var createUrl = getErpNextUrl() + "/api/resource/Customer";
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

    private void createSalesOrderFallback(Order order, Exception e) {
        log.error("ERPNext circuit breaker activated. Fallback triggered for Order: {}. Reason: {}",
                order.getOrderId(), e.getMessage());
    }

    @CircuitBreaker(name = "erpnext")
    @Retry(name = "erpnext")
    public void syncProductsFromERPNext() {
        if (getApiKey() == null || getApiKey().isEmpty())
            return;

        String lockKey = "sync:erpnext:products";
        boolean locked = lockService.tryLock(lockKey, Duration.ofMinutes(10));

        if (!locked) {
            log.warn("Sync job already running for this tenant. Skipping.");
            return;
        }

        try {
            var url = getErpNextUrl() + "/api/resource/Item"
                    + "?fields=[\"item_code\",\"item_name\",\"description\",\"standard_rate\",\"image\"]"
                    + "&filters=[[\"Item\",\"disabled\",\"=\",0]]";
            // We could also filter by Item Group if each tenant has one:
            // + "&filters=[[\"Item\",\"item_group\",\"=\",\"" +
            // credentialProvider.getTenantId() + "\"]]"

            var response = restClient.get()
                    .uri(url)
                    .header("Authorization", getAuthHeader())
                    .retrieve()
                    .body(String.class);

            var root = objectMapper.readTree(response);
            var data = root.get("data");
            List<ProductDTO> products = new ArrayList<>();
            if (data.isArray()) {
                for (var node : data) {
                    var itemCode = node.get("item_code").asText();
                    var p = new ProductDTO(
                            (long) itemCode.hashCode(),
                            node.get("item_name").asText(),
                            itemCode,
                            node.has("image") ? node.get("image").asText() : "default.png",
                            node.has("description") ? node.get("description").asText() : "",
                            0,
                            node.has("standard_rate") ? node.get("standard_rate").asDouble() : 0.0,
                            0.0,
                            node.has("standard_rate") ? node.get("standard_rate").asDouble() : 0.0,
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            null,
                            new java.util.HashMap<>(),
                            null,
                            new java.util.HashMap<>());
                    products.add(p);
                }
            }
            // if (searchService != null) {
            // searchService.indexProducts(products);
            // }
            log.info("Synced {} items from ERPNext.", products.size());
        } catch (Exception e) {
            log.error("Failed to sync products from ERPNext: {}", e.getMessage());
        } finally {
            lockService.unlock(lockKey);
        }
    }

    public void updateOrderStatuses() {
        if (getApiKey() == null || getApiKey().isEmpty())
            return;

        var pendingOrders = orderRepo.findOngoingOrders();

        for (var order : pendingOrders) {
            try {
                var erpSalesOrderUrl = getErpNextUrl() + "/api/resource/Sales Order";
                var url = erpSalesOrderUrl + "/" + order.getErpNextOrderName();
                var response = restClient.get()
                        .uri(url)
                        .header("Authorization", getAuthHeader())
                        .retrieve()
                        .onStatus(s -> s.value() == 404, (req, res) -> {
                            log.warn("Order {} not found in ERPNext. Skipping.", order.getErpNextOrderName());
                        })
                        .body(JsonNode.class);

                if (response != null && response.has("data")) {
                    var erpStatus = response.get("data").get("status").asText();
                    var localStatus = mapErpStatus(erpStatus);
                    processStatusChange(order, localStatus, response.get("data"));
                }
            } catch (Exception e) {
                log.error("Error fetching status for order {}: {}", order.getOrderId(), e.getMessage());
            }
        }

    }

    @Transactional
    public void handleWebhookStatusUpdate(String erpOrderName, String erpStatus, JsonNode fullData) {
        orderRepo.findByErpNextOrderName(erpOrderName).ifPresent(order -> {
            OrderStatus localStatus = mapErpStatus(erpStatus);
            if (localStatus != order.getOrderStatus()) {
                processStatusChange(order, localStatus, fullData);
                log.info("Webhook: Order {} status updated to {}", order.getOrderId(), localStatus);
            }
        });
    }

    private void processStatusChange(Order order, OrderStatus newStatus, JsonNode data) {
        order.setOrderStatus(newStatus);

        if (data != null && data.has("tracking_number")) {
            updateShipmentInfo(order, data);
        }

        orderRepo.save(order);

        try {
            String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
            Shipment shipment = shipmentRepo.findByOrderId(order.getOrderId());
            var event = new OrderStatusEvent(
                    order.getOrderId(), order.getEmail(), newStatus.name(),
                    (shipment != null) ? shipment.getAwbNumber() : null,
                    (shipment != null) ? shipment.getCarrier() : null,
                    tenantId);
            eventProducer.publish("order_status_events", event);
        } catch (Exception px) {
            log.error("Failed to publish status event: {}", px.getMessage());
        }
    }

    @CircuitBreaker(name = "erpnext")
    @Retry(name = "erpnext")
    public void cancelSalesOrder(String erpOrderName) {
        if (getApiKey() == null || getApiKey().isEmpty())
            return;
        try {
            var url = getErpNextUrl() + "/api/resource/Sales Order/" + erpOrderName;
            Map<String, Object> body = new HashMap<>();
            body.put("status", "Cancelled");

            restClient.put()
                    .uri(url)
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Order {} cancelled in ERPNext.", erpOrderName);
        } catch (Exception e) {
            log.error("Error cancelling Sales Order in ERPNext: {}", e.getMessage());
        }
    }

    @CircuitBreaker(name = "erpnext")
    @Retry(name = "erpnext")
    public void cancelSalesOrderByClientOrderId(String clientOrderId) {
        if (getApiKey() == null || getApiKey().isEmpty())
            return;

        try {
            // Find map using po_no
            String url = getErpNextUrl() + "/api/resource/Sales Order?filters=[[\"Sales Order\",\"po_no\",\"=\",\""
                    + clientOrderId + "\"]]";
            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", getAuthHeader())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            if (root.has("data") && root.get("data").isArray() && root.get("data").size() > 0) {
                String erpOrderName = root.get("data").get(0).get("name").asText();
                cancelSalesOrder(erpOrderName);
            } else {
                log.warn("No ERPNext Sales Order found for Client Order ID: {}", clientOrderId);
            }
        } catch (Exception e) {
            log.error("Error resolving/cancelling Sales Order by Client ID {}: {}", clientOrderId, e.getMessage());
        }
    }

    private void updateShipmentInfo(Order order, JsonNode salesOrderData) {
        try {
            var shipment = shipmentRepo.findByOrderId(order.getOrderId());
            if (shipment == null) {
                shipment = new Shipment();
                shipment.setOrderId(order.getOrderId());
                shipment.setCarrier("ERPNext");
                shipment.setStatus("SHIPPED");
                // order.setShipment(shipment); // Removed bidirectional link handling
            }

            if (salesOrderData.has("tracking_number")) {
                shipment.setAwbNumber(salesOrderData.get("tracking_number").asText());
            }
            if (salesOrderData.has("courier_name")) {
                shipment.setCourierName(salesOrderData.get("courier_name").asText());
            }

            if (salesOrderData.has("delivery_note")) {
                shipment.setExternalShipmentId(salesOrderData.get("delivery_note").asText());
            }

            shipmentRepo.save(shipment);
            log.info("Updated Shipment info for Order {} from ERPNext", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to update shipment info from ERPNext: {}", e.getMessage());
        }
    }

    private OrderStatus mapErpStatus(String erpStatus) {
        return switch (erpStatus.toUpperCase()) {
            case "COMPLETED" -> OrderStatus.DELIVERED;
            case "CANCELLED" -> OrderStatus.CANCELLED;
            case "DRAFT" -> OrderStatus.PENDING;
            case "ON HOLD" -> OrderStatus.PROCESSING; // Fallback
            default -> OrderStatus.PROCESSING;
        };
    }

    /**
     * Trigger purchase order in ERPNext for inventory restock.
     * 
     * @param productId Product ID
     * @param quantity  Quantity to order
     * @return Purchase order ID if successful, null otherwise
     */
    public String triggerPurchaseOrder(Long productId, int quantity) {
        if (getApiKey() == null || getApiKey().isEmpty()) {
            log.warn("ERPNext API not configured, skipping purchase order creation");
            return null;
        }

        try {
            var poUrl = getErpNextUrl() + "/api/resource/Purchase Order";
            Map<String, Object> purchaseOrder = new HashMap<>();
            purchaseOrder.put("supplier", "Default Supplier"); // TODO: Make configurable
            purchaseOrder.put("transaction_date", java.time.LocalDate.now().toString());
            purchaseOrder.put("schedule_date", java.time.LocalDate.now().plusDays(7).toString());

            // Add item
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("item_code", "ITEM-" + productId); // TODO: Get actual item code
            item.put("qty", quantity);
            item.put("rate", 100.0); // TODO: Get actual rate
            items.add(item);
            purchaseOrder.put("items", items);

            var response = restClient.post()
                    .uri(poUrl)
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(purchaseOrder)
                    .retrieve()
                    .toEntity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                String poName = (String) data.get("name");
                log.info("Purchase order {} created in ERPNext for product {}", poName, productId);
                return poName;
            }
        } catch (Exception e) {
            log.error("Error creating purchase order in ERPNext: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Create or update item in ERPNext from product event.
     * 
     * @param event Product created or updated event
     */
    public void createOrUpdateItem(Object event) {
        if (getApiKey() == null || getApiKey().isEmpty()) {
            log.warn("ERPNext API not configured, skipping item sync");
            return;
        }

        try {
            String itemCode = null;
            String itemName = null;

            if (event instanceof com.app.core.events.ProductCreatedEvent created) {
                itemCode = created.itemCode();
                itemName = created.productName();
            } else if (event instanceof com.app.core.events.ProductUpdatedEvent updated) {
                itemCode = updated.itemCode();
                itemName = "Updated Product"; // TODO: Get name from event
            } else {
                log.warn("Unknown event type for item sync: {}", event.getClass().getName());
                return;
            }

            var itemUrl = getErpNextUrl() + "/api/resource/Item/" + itemCode;
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("item_code", itemCode);
            itemData.put("item_name", itemName);
            itemData.put("item_group", "Products");
            itemData.put("stock_uom", "Nos");

            // Try to update first, if not exists then create
            try {
                restClient.put()
                        .uri(itemUrl)
                        .header("Authorization", getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(itemData)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Item {} updated in ERPNext", itemCode);
            } catch (Exception updateError) {
                // If update fails, try to create
                restClient.post()
                        .uri(getErpNextUrl() + "/api/resource/Item")
                        .header("Authorization", getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(itemData)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Item {} created in ERPNext", itemCode);
            }
        } catch (Exception e) {
            log.error("Error syncing item to ERPNext: {}", e.getMessage(), e);
        }
    }

    /**
     * Deactivate item in ERPNext.
     * 
     * @param itemCode Item code to deactivate
     */
    public void deactivateItem(String itemCode) {
        if (getApiKey() == null || getApiKey().isEmpty()) {
            log.warn("ERPNext API not configured, skipping item deactivation");
            return;
        }

        try {
            var itemUrl = getErpNextUrl() + "/api/resource/Item/" + itemCode;
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("disabled", 1);

            restClient.put()
                    .uri(itemUrl)
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(itemData)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Item {} deactivated in ERPNext", itemCode);
        } catch (Exception e) {
            log.error("Error deactivating item {} in ERPNext: {}", itemCode, e.getMessage(), e);
        }
    }
}
