# Architecture & Governance

> [!IMPORTANT]
> **GOVERNANCE RULE**: All architectural decisions and code changes MUST align with [STRATEGY_ANALYSIS.md](STRATEGY_ANALYSIS.md).
>
> **Core Pillars:**
> 1.  **Async Write-Behind**: All heavy writes (ERPNext Sync) must be decoupled via **DragonflyDB Streams**.
> 2.  **Native Search**: Use ParadeDB (`to_tsvector`) for search; do not implement inefficient JPA/Python search logic.
> 3.  **Unified Bus**: Use the unified `EventProducer` for domain events.

## High-Level Overview

This document outlines the architectural roles and responsibilities of the key data subsystems in the Vasu E-Commerce Platform. The architecture follows a **Command Query Responsibility Segregation (CQRS)** inspired pattern, where business operations and high-performance reads are decoupled.

    subgraph "Performance & Intelligence Layer"
        JavaApp --"Full Text Search / Analytics"--> ParadeDB[ParadeDB (Postgres)]
        JavaApp --"Cache / Session / Carts"--> Dragonfly[DragonflyDB (Redis)]
        JavaApp --"Autonomous Intelligence"--> Intelligence[Self-Healing & Anomaly Engine]
    end

---

### 1. Core & Infrastructure Layer (The Foundation)
*   **`modulith-kernel`**: Core plumbing, Scoped Values, Multitenancy switches, and SMILE serialization.
*   **`modulith-governance`**: Central nervous system—Audit logs, Rule Engine (SpEL), and Domain State Machines.
*   **`modulith-security`**: Identity Provider (OIDC/OAuth2), RBAC, and Token management.
*   **`modulith-erp-sync`**: Strictly isolated ERPNext gateway and webhook orchestration.

### 2. Autonomous Intelligence Layer (The Brain)
*   **`modulith-intelligence`**: Self-Healing Catalog, Dynamic Pricing engines, and Predictive Analytics.
*   **`modulith-discovery`**: Semantic Search, Visual Search (Vector DJL), and AI-driven ranking.
*   **`modulith-marketing`**: Segment-aware Journeys, WhatsApp/Email automation, and RFM calculation.

### 3. Transactional Commerce Layer (The Engine)
*   **`modulith-catalog`**: Product Master, Category hierarchies, and Inventory Master.
*   **`modulith-cart`**: High-performance, Redis-backed carts with abandonment tracking.
*   **`modulith-checkout`**: Parallelized validation pipeline (`OptimizedCheckoutService`).
*   **`modulith-order`**: Core order lifecycle, fulfillment state transitions.
*   **`modulith-finance`**: Payments (Razorpay/Hyperswitch), Taxes, and Promotion evaluation.
*   **`modulith-logistics`**: Warehouse routing, Carrier management, and Pincode latency.
*   **`modulith-support`**: Ticketing, Autonomous Anomaly Escalation, and Returns.

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
