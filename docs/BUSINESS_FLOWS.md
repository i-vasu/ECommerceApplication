# Business Flows & API Ecosystem - Fashion Modular Monolith

This document provides a comprehensive map of the end-to-end business flows within the consolidated Fashion E-Commerce application. The system is architected using **Spring Modulith**, providing domain isolation with asynchronous event-driven communication.

---

## 1. Unified Authentication & Identity Lifecycle
*Managed by: `modulith-identity` and `modulith-order`*

*   **User Onboarding Flow**:
    *   **Process**: User Registration → Password Complexity Validation → Secure BCrypt Hashing → Persistence (Unique Email/Mobile validation) → `UserRegisteredEvent` Publication → Async Email Verification (24h Expiry) Dispatch.
    *   **Hardening**: Mobile number uniqueness enforced; 24h verification window; Mandatory password complexity (Upper/Lower/Digit/Special).
    *   **Key APIs**: `POST /api/v1/register`, `POST /api/v1/verify-email`.
*   **Adaptive Security Login**:
    *   **Process**: Multitenant Credential Verification → Stateless JWT Issuance → Opaque Refresh Token stored in Redis.
    *   **Key APIs**: `POST /api/v1/login`, `POST /api/v1/refresh-token`.
*   **Automated Account Recovery**:
    *   **Process**: Reset Request → Secure Token Generation → Email Delivery → Expiration-aware Token Validation → Password Update.
    *   **Key APIs**: `POST /api/v1/forgot-password`, `POST /api/v1/reset-password`.

---

## 2. Intelligent Product Discovery & Catalog
*Managed by: `modulith-product`*

*   **AI Visual Search Flow**:
    *   **Process**: Image Upload → Validation (Format: JPEG/PNG, Max Size: 5MB) → DJL (Deep Java Library) Image Vectorization → Vector Similarity Search (ParadeDB/pgvector) → Returning Top-N matching fashion items.
    *   **Hardening**: Strict MIME check (image/jpeg, image/png); Maximum file size limit (5MB) enforced via `AppConstants`.
    *   **Key APIs**: `POST /api/v1/search/visual`.
*   **Optimized Catalog Browsing**:
    *   **Process**: Product Fetch → Pagination Size Validation → **Jackson 3 SMILE (Binary)** Serialization → Multi-level Caching → Async View Tracking (`AnalyticsService`).
    *   **Hardening**: Resource exhaustion protection by capping `pageSize` (Max: 50); Keyword search efficiency enforced (Min: 3 chars); SMILE binary format for optimized low-latency data transfer.
    *   **Key APIs**: `GET /api/public/products`, `GET /api/public/products/keyword`, `GET /api/public/products/trending`.
*   **Dynamic Personalization Flow**:
    *   **Process**: Tracking user behavior → Updating Redis-based "Recently Viewed" and "Search History" sets → Real-time Recommendation generation.
    *   **Key APIs**: `GET /api/personalization/recently-viewed`, `GET /api/personalization/best-sellers`.

---

## 3. High-Performance Cart & Transaction Pipeline
*Managed by: `modulith-order`*

*   **Reactive Cart Management**:
    *   **Process**: Item Addition → Real-time Stock Reservation (Redis Atomic Ops) → Tax/Shipping Calculation → Dynamic Coupon Validation.
    *   **Key APIs**: `POST /api/public/carts/{id}/products/{id}`, `POST /api/public/carts/{id}/coupon/{code}`.
*   **Parallel Validation Checkout**:
    *   **Process**: `OptimizedCheckoutService` triggers parallel threads for (Address Integrity, Flash Inventory Lock, Pricing Consistency, Coupon Validity) → Result Aggregation → Final Order Confirmation.

---

## 4. Multi-Tenant ERPNext Synchronization
*Managed by: `modulith-service`*

*   **Scheduled Background Sync**:
    *   **Process**: `SyncScheduler` (distributed) → Acquires `RedisLock` per Tenant → Delta Fetch from ERPNext REST API → Local Database/Search Index Update.
    *   **Key APIs**: `POST /api/admin/products/sync` (Manual Force Sync).
*   **Tenant Provisioning**:
    *   **Process**: Fashion Brand Onboarding → `ERPNextSetupService` execution → Custom DocType creation (Size Charts, Style Guides) on target ERPNext site.
    *   **Key APIs**: `POST /api/admin/setup/erpnext`.

---

## 5. Order Fulfillment & Financial Lifecycle
*Managed by: `modulith-order`*

*   **Razorpay Payment Integration**:
    *   **Process**: Internal Order Creation (State: PENDING) → Tokenized Razorpay Order Issuance → Webhook/Signature Verification → `OrderPaidEvent` Publication.
    *   **Key APIs**: `POST /api/v1/create/{orderId}`, `POST /api/v1/verify`.
*   **Supply Chain Ingestion**:
    *   **Process**: `OrderPaidEvent` received → Inventory module confirms stock deduction → ERPNext Invoicing → Marketplace updates (Amazon/Flipkart) via internal ingestion service.
    *   **Key APIs**: `POST /api/v1/admin/ingestOrder`.

---

## 6. Post-Purchase Experience & Social Proof
*Managed by: `modulith-product` and `modulith-service`*

*   **Logistics & Shipment Tracking**:
    *   **Process**: Shipment creation in ERP → Logistics Provider Webhook Reception → Internal Shipment State Machine Update → Customer Notification dispatch.
    *   **Key APIs**: `GET /api/v1/shipments/track/{trackingNumber}`.
*   **Verified Review Ecosystem**:
    *   **Process**: User review submission → Cross-reference with Redis `user:purchases` set (populated by `OrderCompletedEvent`) → "Verified Purchase" badge assignment.
    *   **Key APIs**: `POST /api/v1/products/{id}/reviews`.
*   **Customer Support Ticketing**:
    *   **Process**: Issue Logging → Support Agent Dashboard Routing (Service Module) → SLA Tracking → Resolution notification.
    *   **Key APIs**: `POST /api/v1/createTicket`, `POST /api/v1/replyToTicket`.

---

## Technical Edge Case Handling
1.  **Distributed Lock**: Background syncs use Redis locks to prevent race conditions across server nodes.
2.  **Binary Serialization**: All intra-service signals and caches use **Jackson 3 SMILE**, reducing network payload by ~40%.
3.  **Atomic Inventory**: Inventory is reserved *before* order confirmation to prevent overselling during high-concurrency "Flash Drops".
4.  **Schema-on-Write Search**: Product changes trigger async search index refreshes for near real-time catalog accuracy.
5.  **API Resource Hardening**: All paginated endpoints (Products, Orders, Support) enforce a strict maximum page size (50) to prevent memory-based Denial of Service (DoS). Visual search handles strict file-type and size validation at the controller entry point.

Next ToDO: 

ERPNext Side: Create a Webhook in ERPNext all DocType as needed.
Endpoint: https://your-domain.com/api/webhooks/erpnext/order-status
Secret: Generate a random string and save it in both ERPNext (Webhook Secret field) and our tenants table (erpNextWebhookSecret column).

1. ⚡ Flash Sale & High-Concurrency Engine
In fashion, "New Drops" create massive traffic spikes. Currently, our inventory check happens during checkout.

What we can do: Transition to a Redis-first Atomic Inventory. During a flash sale, stock is decremented in Redis (DECR) in milliseconds. This prevents "Overselling" and ensures the database isn't crushed by 10,000 users hitting the same item simultaneously.
2. 🎟️ Advanced Promotions & Coupon Engine
Currently, we have basic coupon support. Real-world fashion stores need:

Automatic Rules: "Buy 2 Get 1 Free" or "10% off on all Blue Dresses."
Stackable Coupons: Allowing a "First Purchase" discount to stack with a "Free Shipping" coupon.
Cart Price Rules: "Add ₹500 more to unlock Free Delivery."


4. 🚚 Intelligent Warehouse Routing
We added erpNextWarehouse to the Tenant entity, but what if a tenant has multiple warehouses?

Smart Sourcing: Automatically selecting the warehouse closest to the customer's PIN code to reduce shipping costs and delivery time.
5. 🤳 Personalized Style Feeds (AI)
We've already implemented Visual and Semantic search.

The Next Level: A "Because you liked [Product A]" engine that uses our vector embeddings to show visually similar items on the product page, increasing the Average Order Value (AOV).
📱 6. Omnichannel Notifications (SMS/WhatsApp)
Emails often go to spam.


