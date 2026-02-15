# WhatsApp Flows & Zoho Integration Guide

## 1. Zoho Books API Integration Status
We have successfully integrated the following high-priority features using the Zoho Books REST API. These features are implemented in `ZohoClient.java` and `ZohoSyncService.java`.

### **Implemented Features:**
| Feature | Implementation Details | Trigger Event |
| :--- | :--- | :--- |
| **Sales Orders** | Creates a Sales Order in Zoho when an order is placed in our system. | `OrderCreatedEvent` |
| **Purchase Orders** | Creates a PO in Zoho. Methods to update status and mark as received. | `PurchaseOrderCreatedEvent` |
| **Estimates (Quotes)** | Creates an Estimate for B2B requests. Can be converted to Sales Order. | `QuoteRequestedEvent` |
| **Payments** | Records Customer Payments (for Sales) and Vendor Payments (for Bills). | `OrderPaidEvent` |
| **Tax Management** | Methods to fetch and create tax rates (`getTaxes`, `createTax`). | On Demand / Config |
| **Expenses** | Records expenses in Zoho. | `ExpenseIncurredEvent` |
| **Bank Reconciliation** | Methods to fetch bank accounts, import transactions, and match them. | Periodic / Manual |
| **Inventory Adjustments** | Syncs manual stock corrections (shrinkages, audits) to Zoho. | `StockUpdatedEvent` (Source: MANUAL/CORRECTION) |
| **Vendor Credits** | Creates Vendor Credits in Zoho when goods are returned to suppliers. | `VendorReturnEvent` |
| **Manual Journals** | Syncs complex accounting entries (depreciation, payroll) to Zoho. | `JournalEntryPostedEvent` |

### **Configuration:**
Ensure the following properties are set in `application.properties`:
```properties
zoho.books.enabled=true
zoho.books.client-id=YOUR_CLIENT_ID
zoho.books.client-secret=YOUR_CLIENT_SECRET
zoho.books.refresh-token=YOUR_REFRESH_TOKEN
zoho.books.organization-id=YOUR_ORG_ID
```

---

## 2. Implementing WhatsApp Flows
WhatsApp Flows allow you to build structured, interactive forms (like appointment booking, feedback, or return requests) directly within the WhatsApp interface.

### **Architecture:**
1.  **Meta Business Manager**: You define the Flow UI (JSON layout) here.
2.  **WhatsAppGateway**: Sends a message with a "Call to Action" button that opens the Flow.
3.  **Webhook Endpoint**: Receives the data submitted by the user through the Flow.

### **Step-by-Step Implementation Guide:**

#### **Step 1: Create the Flow in Meta Business Manager**
1.  Go to **WhatsApp Manager** > **Flows**.
2.  Create a new Flow (e.g., "Return Request Flow").
3.  Design the UI using the JSON editor.
    *   **Example JSON for a Return Reason form:**
    ```json
    {
      "version": "2.1",
      "screens": [
        {
          "id": "return_reason_screen",
          "title": "Return Request",
          "data": {},
          "layout": {
            "type": "SingleColumnLayout",
            "children": [
              {
                "type": "Form",
                "name": "return_form",
                "children": [
                  {
                    "type": "Dropdown",
                    "name": "reason",
                    "label": "Reason for Return",
                    "options": [
                      { "id": "sizing", "title": "Sizing Issue" },
                      { "id": "damaged", "title": "Damaged Item" },
                      { "id": "other", "title": "Other" }
                    ]
                  },
                  {
                    "type": "Footer",
                    "label": "Submit Request",
                    "on-click-action": {
                      "name": "complete",
                      "payload": {
                        "reason": "${form.reason}"
                      }
                    }
                  }
                ]
              }
            ]
          }
        }
      ]
    }
    ```

#### **Step 2: Send the Flow via API (`WhatsAppGateway`)**
You need to implement a method to send a message that opens this flow.

**Add this to `WhatsAppGateway.java`:**
```java
public void sendFlowMessage(String toPhoneNumber, String flowId, String flowToken, String screenId) {
    Map<String, Object> interactive = new HashMap<>();
    interactive.put("type", "flow");
    interactive.put("header", Map.of("type", "text", "text", "Return Request"));
    interactive.put("body", Map.of("text", "Please select a reason for your return."));
    interactive.put("footer", Map.of("text", "Vaabhi Fashion"));
    
    Map<String, Object> action = new HashMap<>();
    action.put("name", "flow");
    action.put("parameters", Map.of(
        "flow_message_version", "3",
        "flow_token", flowToken, // Unique token to track this specific interaction
        "flow_id", flowId,
        "flow_cta", "Start Return",
        "flow_action", "navigate",
        "flow_action_payload", Map.of(
            "screen", screenId
        )
    ));
    interactive.put("action", action);

    Map<String, Object> body = new HashMap<>();
    body.put("messaging_product", "whatsapp");
    body.put("to", toPhoneNumber);
    body.put("type", "interactive");
    body.put("interactive", interactive);

    sendToMeta(body);
}
```

#### **Step 3: Handle the Response (Webhook)**
When the user submits the flow, Meta sends a webhook to your configured webhook URL.
You need a Controller to handle this.

**Create `WhatsAppWebhookController.java`:**
```java
@PostMapping("/webhooks/whatsapp")
public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
    // Parse JSON payload
    // Extract "nfm_reply" (Native Flow Message Reply)
    // It will contain the JSON data defined in your Flow (e.g., "reason": "sizing")
    
    // Trigger business logic (e.g., create ReturnRequest)
    return ResponseEntity.ok("Received");
}
```

### **Recommendation:**
Start with **Simple Template Messages** (implemented now) before moving to Flows, as Flows require significant configuration in the Meta Developer Portal and Business Manager.

---

## 3. Google Merchant Integration
The Google Merchant API integration is now set up with the correct dependencies in `modulith-marketing`.

### **Status:**
*   **Dependencies**: Integrated (`google-shopping-merchant-products:1.5.0`, `libraries-bom:26.72.0`).
*   **Service**: `GoogleMerchantService.java` is implemented and initializes the `ProductsServiceClient`.
*   **Authentication**: Configured to use Service Account Key from `application.properties`.
*   **Next Steps**: 
    1.  Provide valid `google.merchant.api.merchant-id` and `google.merchant.api.service-account-key` in `application.properties`.
    2.  Uncomment the product creation logic in `GoogleMerchantService` and verify field mapping against the specific `v1beta` library documentation (as method signatures vary slightly between versions).
