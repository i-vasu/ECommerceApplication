# Architecture: Separation of Concerns

This document outlines the architectural roles and responsibilities of the key data subsystems in the Vasu E-Commerce Platform. The architecture follows a **Command Query Responsibility Segregation (CQRS)** inspired pattern, where business operations and high-performance reads are decoupled.

```mermaid
graph TD
    User[Storefront User] -->|Browses/Search| JavaApp[Spring Boot App]
    Admin[Admin User] -->|Manages| ERPNext[ERPNext]
    
    subgraph "Core Data Layer"
        ERPNext --"Syncs Products/Inventory"--> JavaApp
        JavaApp --"Pushes Orders"--> ERPNext
    end

    subgraph "Performance Layer"
        JavaApp --"Full Text Search / Analytics"--> ParadeDB[ParadeDB (Postgres)]
        JavaApp --"Cache / Session / Carts"--> Dragonfly[DragonflyDB (Redis)]
    end
```

---

## 1. ERPNext - The Business Core (Source of Truth)
**Role:** Enterprise Resource Planning & Master Data Management.
**Responsibility:**
- **Product Master:** All products, variants, pricing rules, and images are originally created and managed here.
- **Inventory Management:** Tracks actual stock levels across warehouses.
- **Order Fulfillment:** processing, shipping, and logistics workflow.
- **Accounting:** Generates invoices, tracks revenue, handling tax calculations.

**Data Flow:**
- **Inbound:** Receives `Sales Order` from the Java App when a customer checks out.
- **Outbound:** Syncs Product/Inventory updates to the Java App (Postgres) so the storefront is always up to date.

---

## 2. ParadeDB - The Search & Analytics Engine
**Role:** High-Performance Search & Analytics (PostgreSQL Extension).
**Responsibility:**
- **Full Text Search:** Powers the search bar. Uses BM25 scoring to find "Blue Denim Jacket" instantly.
- **Faceted Filtering:** Handles complex queries like "Size: M AND Color: Blue AND Price < 5000" efficiently.
- **Real-time Analytics:** Aggregates sales trends, view counts, and conversion metrics without impacting the transactional ERP database.

**Why not standard Postgres?**
- ParadeDB creates **BM25 indexes** (similar to Elasticsearch) directly on Postgres tables, eliminating the need to sync data to an external search engine like Elastic/Solr.

---

## 3. DragonflyDB - The High-Speed Cache
**Role:** In-memory Data Store (Redis-compatible).
**Responsibility:**
- **Transient Data:** Stores user Sessions, Shopping Carts (before checkout), and OTPs.
- **Caching:** Caches frequent queries (e.g., "Top 10 Trending Products") to reduce DB load.
- **Rate Limiting:** Protects APIs from abuse.
- **Locks:** Distributed locks to prevent race conditions (e.g., during inventory reservation).

**Why DragonflyDB?**
- Drop-in replacement for Redis but uses a **multi-threaded shared-nothing architecture**, offering significantly higher throughput and lower latency for high-traffic sales events.

---

## 4. Spring Modulith - The Orchestrator
**Role:** The Application Layer (Java).
**Responsibility:**
- Acts as the interface between the User and the subsystems.
- **Sync Logic:** Listens to ERPNext webhooks to update ParadeDB.
- **Fallback Logic:** If Dragonfly is down, falls back to DB. If ParadeDB is slow, serves from Cache.
- **Security:** Handles JWT/OAuth2 authentication (IdP).

## Summary of Data Lifecycle
1.  **Creation:** Admin creates a "Red T-Shirt" in **ERPNext**.
2.  **Sync:** Application syncs "Red T-Shirt" to **PostgreSQL**.
3.  **Index:** **ParadeDB** automatically indexes the description and attributes.
4.  **Browse:** User searches for "T-Shirt". Query hits **ParadeDB**.
5.  **Cart:** User adds to cart. Data stored in **DragonflyDB**.
6.  **Order:** User pays. Order stored in **PostgreSQL** and pushed to **ERPNext** for shipping.
