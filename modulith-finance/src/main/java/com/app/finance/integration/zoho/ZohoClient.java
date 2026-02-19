package com.app.finance.integration.zoho;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class ZohoClient {

    private final ZohoProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private String accessToken;
    private Instant tokenExpiry = Instant.MIN;

    public ZohoClient(ZohoProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    private synchronized String getAccessToken() {
        if (!properties.isEnabled()) {
            return "dummy-token"; 
        }

        if (tokenExpiry.isAfter(Instant.now().plusSeconds(60))) {
            return accessToken;
        }

        log.info("Refreshing Zoho Access Token...");
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("refresh_token", properties.getRefreshToken());
            formData.add("client_id", properties.getClientId());
            formData.add("client_secret", properties.getClientSecret());
            formData.add("grant_type", "refresh_token");

            JsonNode response = restClient.post()
                    .uri(properties.getAuthUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, resp) -> {
                        String body = new String(resp.getBody().readAllBytes());
                        log.error("Zoho Auth Error Response: Status={}, Body={}", resp.getStatusCode(), body);
                        throw new RuntimeException("Zoho Auth API returned error: " + resp.getStatusCode() + " - " + body);
                    })
                    .body(JsonNode.class);

            if (response != null && response.has("access_token")) {
                this.accessToken = response.get("access_token").asText();
                int expiresIn = response.has("expires_in") ? response.get("expires_in").asInt() : 3600;
                this.tokenExpiry = Instant.now().plusSeconds(expiresIn);
                log.info("Zoho Access Token refreshed. Expires in: {} seconds", expiresIn);
                return accessToken;
            } else {
                log.error("Failed to refresh Zoho Token: response missing access_token field. Response: {}", response);
                throw new RuntimeException("Zoho Token Refresh Failed: access_token missing");
            }

        } catch (Exception e) {
            log.error("Critical error during Zoho token refresh", e);
            throw new RuntimeException("Zoho Token Refresh Error: " + e.getMessage(), e);
        }
    }

    public void createItem(Map<String, Object> itemData) {
        if (!properties.isEnabled()) {
            log.info("Zoho Sync Disabled. Skipping Item creation: {}", itemData.get("name"));
            return;
        }

        try {
            String token = getAccessToken();

            // Zoho Books Check if item exists (by SKU/Name) - Simplified: Just Try Create
            // In a real scenario, you'd search first. The API might return error if duplicate.

            String response = restClient.post()
                    .uri(properties.getBaseUrl() + "/items?organization_id=" + properties.getOrganizationId())
                    .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(itemData)
                    .retrieve()
                    .body(String.class);
            
            log.info("Zoho Item Created: {}", response);
        } catch (Exception e) {
            log.error("Failed to create Item in Zoho", e);
            // Don't block main flow, but maybe retry later
        }
    }

    public String findItem(String sku) {
        if (!properties.isEnabled()) return "dummy-item-id";
        try {
            String token = getAccessToken();

            // Search by SKU (item_name contains SKU or strictly SKU field if supported)
            // Zoho API supports filtering by name, description, etc.
            // But strict SKU search might need iteration or specialized filter.
            // Using name/sku search endpoint.
            
            String encodedSku = java.net.URLEncoder.encode(sku, java.nio.charset.StandardCharsets.UTF_8);
            
            // Note: Zoho Books API v3 items?name=... or items?search_text=...
            String response = getFromZoho("/items?search_text=" + encodedSku);
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");
            if (items.isArray() && items.size() > 0) {
                // Return first match
                return items.get(0).get("item_id").asText();
            }
            return null; // Not found
        } catch (Exception e) {
            log.error("Failed to find item by SKU: {}", sku, e);
            return null;
        }
    }


    public String createInvoice(Map<String, Object> invoiceData) {
        if (!properties.isEnabled()) {
            log.info("Zoho Sync Disabled. Skipping Invoice creation.");
            return "dummy-invoice-id";
        }

        try {
            String token = getAccessToken();

            String response = restClient.post()
                    .uri(properties.getBaseUrl() + "/invoices?organization_id=" + properties.getOrganizationId())
                    .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(invoiceData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String invoiceId = root.path("invoice").path("invoice_id").asText(null);
            log.info("Zoho Invoice Created: {}", invoiceId);
            return invoiceId;
        } catch (Exception e) {
            log.error("Failed to create Invoice in Zoho", e);
            return null;
        }
    }


    public String findOrCreateCustomer(String name, String email) {
        if (!properties.isEnabled()) {
            return "dummy-customer-id";
        }

        try {
            String token = getAccessToken();

            // 1. Search for customer
            String searchResponse = restClient.get()
                    .uri(properties.getBaseUrl() + "/contacts?organization_id=" + properties.getOrganizationId() + "&email=" + email)
                    .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
                    .retrieve()
                    .body(String.class);
            
            JsonNode root = objectMapper.readTree(searchResponse);
            JsonNode contacts = root.path("contacts");
            if (contacts.isArray() && contacts.size() > 0) {
                return contacts.get(0).get("contact_id").asText();
            }

            // 2. Create customer if not found
            Map<String, Object> customerData = Map.of(
                "contact_name", name,
                "email", email,
                "contact_persons", List.of(Map.of(
                    "first_name", name,
                    "email", email,
                    "is_primary_contact", true
                ))
            );

            String createResponse = restClient.post()
                    .uri(properties.getBaseUrl() + "/contacts?organization_id=" + properties.getOrganizationId())
                    .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(customerData)
                    .retrieve()
                    .body(String.class);
            
            JsonNode createdRoot = objectMapper.readTree(createResponse);
            return createdRoot.path("contact").get("contact_id").asText();

        } catch (Exception e) {
            log.error("Failed to find/create customer in Zoho", e);
            throw new RuntimeException("Zoho Customer Sync Failed", e);
        }
    }

    public String findOrCreateVendor(String name) { // Vendors often don't have email in early PO
        if (!properties.isEnabled()) return "dummy-vendor-id";
        try {
            String token = getAccessToken();
            
            // Search by Name
            String searchResponse = restClient.get()
                    .uri(properties.getBaseUrl() + "/contacts?organization_id=" + properties.getOrganizationId() + "&contact_name=" + name + "&contact_type=vendor")
                    .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(searchResponse);
            JsonNode contacts = root.path("contacts");
            if (contacts.isArray() && contacts.size() > 0) {
                return contacts.get(0).get("contact_id").asText();
            }

            // Create Vendor
            Map<String, Object> vendorData = Map.of(
                "contact_name", name,
                "contact_type", "vendor"
            );
             String createResponse = restClient.post()
                    .uri(properties.getBaseUrl() + "/contacts?organization_id=" + properties.getOrganizationId())
                    .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vendorData)
                    .retrieve()
                    .body(String.class);
            
            JsonNode createdRoot = objectMapper.readTree(createResponse);
            return createdRoot.path("contact").get("contact_id").asText();
        } catch (Exception e) {
            log.error("Failed to find/create vendor in Zoho", e);
            throw new RuntimeException("Zoho Vendor Sync Failed");
        }
    }

    public void createBill(Map<String, Object> billData) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/bills", billData);
        } catch (Exception e) {
            log.error("Failed to create Bill", e);
        }
    }

    public void createCreditNote(Map<String, Object> cnData) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/creditnotes", cnData);
        } catch (Exception e) {
            log.error("Failed to create Credit Note", e);
        }
    }
    
    // Helper to reduce duplication
    // Helper to reduce duplication
    private String appendOrgId(String endpoint) {
        String separator = endpoint.contains("?") ? "&" : "?";
        return properties.getBaseUrl() + endpoint + separator + "organization_id=" + properties.getOrganizationId();
    }

    private String postToZoho(String endpoint, Map<String, Object> data) throws Exception {
        String token = getAccessToken();
        return restClient.post()
            .uri(appendOrgId(endpoint))
            .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(data)
            .retrieve()
            .body(String.class);
    }

    private String getFromZoho(String endpoint) throws Exception {
        String token = getAccessToken();
        return restClient.get()
            .uri(appendOrgId(endpoint))
            .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
            .retrieve()
            .body(String.class);
    }

    private void putToZoho(String endpoint, Map<String, Object> data) throws Exception {
        String token = getAccessToken();
        restClient.put()
            .uri(appendOrgId(endpoint))
            .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(data)
            .retrieve()
            .body(String.class);
    }

    // ==================== PAYMENT RECORDING ====================

    /**
     * Record customer payment (when order is paid)
     */
    public void recordCustomerPayment(Map<String, Object> paymentData) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/customerpayments", paymentData);
            log.info("Customer payment recorded in Zoho");
        } catch (Exception e) {
            log.error("Failed to record customer payment", e);
        }
    }

    /**
     * Record vendor payment (when bill is paid)
     */
    public void recordVendorPayment(Map<String, Object> paymentData) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/vendorpayments", paymentData);
            log.info("Vendor payment recorded in Zoho");
        } catch (Exception e) {
            log.error("Failed to record vendor payment", e);
        }
    }

    // ==================== PURCHASE ORDERS ====================

    /**
     * Create purchase order
     */
    public String createPurchaseOrder(Map<String, Object> poData) {
        if (!properties.isEnabled()) return "dummy-po-id";
        try {
            String response = (String) postToZoho("/purchaseorders", poData);
            JsonNode root = objectMapper.readTree(response);
            String poId = root.path("purchaseorder").get("purchaseorder_id").asText();
            log.info("Purchase Order created in Zoho: {}", poId);
            return poId;
        } catch (Exception e) {
            log.error("Failed to create purchase order", e);
            throw new RuntimeException("Zoho PO creation failed", e);
        }
    }

    /**
     * Update purchase order status
     */
    public void updatePurchaseOrderStatus(String poId, String status) {
        if (!properties.isEnabled()) return;
        try {
            Map<String, Object> data = Map.of("status", status);
            putToZoho("/purchaseorders/" + poId, data);
            log.info("Purchase Order {} updated to status: {}", poId, status);
        } catch (Exception e) {
            log.error("Failed to update PO status", e);
        }
    }

    /**
     * Mark PO as received
     */
    public void markPurchaseOrderReceived(String poId) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/purchaseorders/" + poId + "/status/received", Map.of());
            log.info("Purchase Order {} marked as received", poId);
        } catch (Exception e) {
            log.error("Failed to mark PO as received", e);
        }
    }

    // ==================== ESTIMATES/QUOTES ====================

    /**
     * Create estimate/quote
     */
    public String createEstimate(Map<String, Object> estimateData) {
        if (!properties.isEnabled()) return "dummy-estimate-id";
        try {
            String response = (String) postToZoho("/estimates", estimateData);
            JsonNode root = objectMapper.readTree(response);
            String estimateId = root.path("estimate").get("estimate_id").asText();
            log.info("Estimate created in Zoho: {}", estimateId);
            return estimateId;
        } catch (Exception e) {
            log.error("Failed to create estimate", e);
            throw new RuntimeException("Zoho Estimate creation failed", e);
        }
    }

    /**
     * Mark estimate as accepted
     */
    public void markEstimateAccepted(String estimateId) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/estimates/" + estimateId + "/status/accepted", Map.of());
            log.info("Estimate {} marked as accepted", estimateId);
        } catch (Exception e) {
            log.error("Failed to mark estimate as accepted", e);
        }
    }

    /**
     * Convert estimate to sales order
     */
    public String convertEstimateToSalesOrder(String estimateId) {
        if (!properties.isEnabled()) return "dummy-so-id";
        try {
            String response = (String) postToZoho("/estimates/" + estimateId + "/convertto/salesorder", Map.of());
            JsonNode root = objectMapper.readTree(response);
            String soId = root.path("salesorder").get("salesorder_id").asText();
            log.info("Estimate {} converted to Sales Order: {}", estimateId, soId);
            return soId;
        } catch (Exception e) {
            log.error("Failed to convert estimate to sales order", e);
            throw new RuntimeException("Estimate conversion failed", e);
        }
    }

    // ==================== SALES ORDERS ====================

    /**
     * Create sales order
     */
    public String createSalesOrder(Map<String, Object> soData) {
        if (!properties.isEnabled()) return "dummy-so-id";
        try {
            String response = (String) postToZoho("/salesorders", soData);
            JsonNode root = objectMapper.readTree(response);
            String soId = root.path("salesorder").get("salesorder_id").asText();
            log.info("Sales Order created in Zoho: {}", soId);
            return soId;
        } catch (Exception e) {
            log.error("Failed to create sales order", e);
            throw new RuntimeException("Zoho SO creation failed", e);
        }
    }

    /**
     * Mark sales order as confirmed
     */
    public void confirmSalesOrder(String soId) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/salesorders/" + soId + "/status/confirmed", Map.of());
            log.info("Sales Order {} confirmed", soId);
        } catch (Exception e) {
            log.error("Failed to confirm sales order", e);
        }
    }

    /**
     * Convert sales order to invoice
     */
    public String convertSalesOrderToInvoice(String soId) {
        if (!properties.isEnabled()) return "dummy-invoice-id";
        try {
            String response = (String) postToZoho("/salesorders/" + soId + "/convertto/invoice", Map.of());
            JsonNode root = objectMapper.readTree(response);
            String invoiceId = root.path("invoice").get("invoice_id").asText();
            log.info("Sales Order {} converted to Invoice: {}", soId, invoiceId);
            return invoiceId;
        } catch (Exception e) {
            log.error("Failed to convert SO to invoice", e);
            throw new RuntimeException("SO conversion failed", e);
        }
    }

    // ==================== TAX MANAGEMENT ====================

    /**
     * Get all tax rates
     */
    public List<Map<String, Object>> getTaxes() {
        if (!properties.isEnabled()) return Collections.emptyList();
        try {
            String response = getFromZoho("/taxes");
            JsonNode root = objectMapper.readTree(response);
            JsonNode taxes = root.path("taxes");
            
            List<Map<String, Object>> taxList = new java.util.ArrayList<>();
            if (taxes.isArray()) {
                taxes.forEach(tax -> {
                    Map<String, Object> taxMap = new HashMap<>();
                    taxMap.put("tax_id", tax.get("tax_id").asText());
                    taxMap.put("tax_name", tax.get("tax_name").asText());
                    taxMap.put("tax_percentage", tax.get("tax_percentage").asDouble());
                    taxList.add(taxMap);
                });
            }
            return taxList;
        } catch (Exception e) {
            log.error("Failed to get taxes", e);
            return Collections.emptyList();
        }
    }

    /**
     * Create custom tax rate
     */
    public String createTax(Map<String, Object> taxData) {
        if (!properties.isEnabled()) return "dummy-tax-id";
        try {
            String response = (String) postToZoho("/taxes", taxData);
            JsonNode root = objectMapper.readTree(response);
            String taxId = root.path("tax").get("tax_id").asText();
            log.info("Tax created in Zoho: {}", taxId);
            return taxId;
        } catch (Exception e) {
            log.error("Failed to create tax", e);
            throw new RuntimeException("Tax creation failed", e);
        }
    }

    // ==================== EXPENSES ====================

    /**
     * Create expense
     */
    public void createExpense(Map<String, Object> expenseData) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/expenses", expenseData);
            log.info("Expense created in Zoho");
        } catch (Exception e) {
            log.error("Failed to create expense", e);
        }
    }

    // ==================== BANK RECONCILIATION ====================

    /**
     * Get all bank accounts
     */
    public List<Map<String, Object>> getBankAccounts() {
        if (!properties.isEnabled()) return Collections.emptyList();
        try {
            String response = getFromZoho("/bankaccounts");
            JsonNode root = objectMapper.readTree(response);
            JsonNode accounts = root.path("bankaccounts");
            
            List<Map<String, Object>> accountList = new java.util.ArrayList<>();
            if (accounts.isArray()) {
                accounts.forEach(account -> {
                    Map<String, Object> accMap = new HashMap<>();
                    accMap.put("account_id", account.get("account_id").asText());
                    accMap.put("account_name", account.get("account_name").asText());
                    accMap.put("account_number", account.get("account_number").asText());
                    accMap.put("balance", account.get("balance").asDouble());
                    accountList.add(accMap);
                });
            }
            return accountList;
        } catch (Exception e) {
            log.error("Failed to get bank accounts", e);
            return Collections.emptyList();
        }
    }

    /**
     * Import bank transaction
     */
    public void importBankTransaction(String accountId, Map<String, Object> transactionData) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/bankaccounts/" + accountId + "/transactions", transactionData);
            log.info("Bank transaction imported to Zoho");
        } catch (Exception e) {
            log.error("Failed to import bank transaction", e);
        }
    }

    /**
     * Match bank transaction with payment
     */
    public void matchBankTransaction(String transactionId, Map<String, Object> matchData) {
        if (!properties.isEnabled()) return;
        try {
            postToZoho("/banktransactions/" + transactionId + "/match", matchData);
            log.info("Bank transaction {} matched", transactionId);
        } catch (Exception e) {
            log.error("Failed to match bank transaction", e);
        }
    }


    // ==================== CHART OF ACCOUNTS & JOURNALS ====================

    /**
     * Get Chart of Accounts
     */
    public List<Map<String, Object>> getChartOfAccounts() {
        if (!properties.isEnabled()) return Collections.emptyList();
        try {
            String response = getFromZoho("/chartofaccounts");
            JsonNode root = objectMapper.readTree(response);
            JsonNode accounts = root.path("chartofaccounts");
            
            List<Map<String, Object>> accountList = new java.util.ArrayList<>();
            if (accounts.isArray()) {
                accounts.forEach(account -> {
                    Map<String, Object> accMap = new HashMap<>();
                    accMap.put("account_id", account.get("account_id").asText());
                    accMap.put("account_name", account.get("account_name").asText());
                    accMap.put("account_type", account.get("account_type").asText());
                    accountList.add(accMap);
                });
            }
            return accountList;
        } catch (Exception e) {
            log.error("Failed to get Chart of Accounts", e);
            return Collections.emptyList();
        }
    }

    /**
     * Create Manual Journal
     */
    public void createJournal(Map<String, Object> journalData) {
        if (!properties.isEnabled()) return;
        try {
            String response = postToZoho("/journals", journalData);
            log.info("Journal Entry created in Zoho: {}", response);
        } catch (Exception e) {
            log.error("Failed to create Journal Entry", e);
        }
    }

    // ==================== INVENTORY ADJUSTMENTS ====================

    /**
     * Adjust Inventory
     */
    public void adjustInventory(Map<String, Object> adjustmentData) {
        if (!properties.isEnabled()) return;
        try {
            String response = postToZoho("/inventoryadjustments", adjustmentData);
            log.info("Inventory Adjustment created in Zoho: {}", response);
        } catch (Exception e) {
            log.error("Failed to adjust inventory", e);
        }
    }

    // ==================== VENDOR CREDITS ====================

    /**
     * Create Vendor Credit (Return to Vendor)
     */
    public void createVendorCredit(Map<String, Object> creditData) {
        if (!properties.isEnabled()) return;
        try {
            String response = postToZoho("/vendorcredits", creditData);
            log.info("Vendor Credit created in Zoho: {}", response);
        } catch (Exception e) {
            log.error("Failed to create Vendor Credit", e);
        }
    }
}
