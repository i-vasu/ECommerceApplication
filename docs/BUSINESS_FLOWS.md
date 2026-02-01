# Business Flows & API Ecosystem - Fashion Modular Monolith

This document provides a comprehensive map of the end-to-end business flows within the consolidated Fashion E-Commerce application. The system is architected using **Spring Modulith**, providing domain isolation with asynchronous event-driven communication.

---

## 1. Unified Authentication & Identity Lifecycle
*Managed by: `modulith-security` and `modulith-order`*

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
*Managed by: `modulith-catalog`*

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
*Managed by: `modulith-catalog` and `modulith-service`*

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

## 7. Autonomous Business Intelligence & Self-Healing
*Managed by: `modulith-kernel`, `modulith-catalog`, `modulith-security`*

*   **Fraudulent Return Prevention (Fraud Guard)**: Checks historical return rates via SpEL policies to automatically block abusive users.
*   **Self-Healing Catalog**: Daily recalculation of `quality_score` based on return rates, automatically deprioritizing poor-performing items in search.
*   **Dynamic Pricing (Surge/Clearance)**: Real-time price adjustments based on demand velocity (Redis) and inventory levels.
*   **Autonomous Support Escalation**: Automatic ticket creation and AI-categorization for SLA violations detected by the Kernel.
*   **Customer Insight Engine**: Daily RFM analysis to identify VIPs and "At-Risk" users, triggering autonomous recovery journeys.

---

## Technical Edge Case Handling
1.  **Distributed Lock**: Background syncs use Redis locks to prevent race conditions across server nodes.
2.  **Binary Serialization**: All intra-service signals and caches use **Jackson 3 SMILE**, reducing network payload by ~40%.
3.  **Atomic Inventory**: Inventory is reserved *before* order confirmation to prevent overselling.
4.  **Schema-on-Write Search**: Product changes trigger async search index refreshes.
5.  **API Resource Hardening**: All paginated endpoints enforce a strict maximum page size (50).
6.  **Decoupled Analytics**: Activity tracking is published via **Asynchronous Domain Events**, decoupling business logic from tracking latency.

---

## Strategic Roadmap & Gaps
1.  **Transactional Outbox**: Need persistent event storage to prevent data loss on JVM crashes.
2.  **Redis-First Atomic Inventory**: Replace DB-based checks with purely Redis-atomic locks.
3.  **Regional Logistic Routing**: Automatically switch carriers based on real-time pincode-level latency anomalies.
4.  **Segment-Aware Discovery**: Personalize search ranking based on user segment (VIP vs. New User).
5.  **Omnichannel Notifications**: Integration for WhatsApp/SMS status updates.
