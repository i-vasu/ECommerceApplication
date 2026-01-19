# Architecture Assessment: Current Flaws & Risks

This document outlines the identified flaws in the current architecture (Vasu E-Commerce Platform) regarding reliability, scalability, and consistency.

## 1. Synchronous Coupling & Reliability
- **Issue:** The integration with ERPNext (via `ERPNextProductSyncService` and `SyncGateway`) relies heavily on direct HTTP calls or in-memory Spring Integration channels.
- **Risk:** If ERPNext is offline or slow, the Java application's sync threads may block or fail. Data (e.g., failed order syncs) is currently lost if the application restarts before retry, as there is **no persistent message queue** (like RabbitMQ or Kafka) buffering these events.

## 2. Resource Contention (Search vs. Transactions)
- **Issue:** We utilize PostgreSQL for both **Transactional writes** (Orders/Payments) and **Heavy Search Queries** (ParadeDB `to_tsvector` BM25).
- **Risk:** High-traffic search operations (e.g., unexpected marketing surge) will consume CPU/IO on the primary database, potentially slowing down critical `checkout` or `payment` transactions.
- **Missing:** No Read Replica configuration to offload search queries.

## 3. Cache Invalidation Strategy
- **Issue:** `ProductServiceImpl` correctly manages cache eviction for internal updates. However, `ERPNextProductSyncService` updates the database directly (e.g., Stock updates, Variant sync) **without** evicting the high-level `products` page cache.
- **Risk:** Users may see stale product information (e.g., older prices or descriptions) on the listing page even after an update from ERPNext, until the specific cache key expires (TTL). The `inventory:stock` Redis key is updated, but the `getAllProducts` cache is unrelated.

## 4. Secret Management
- **Issue:** `AuthorizationServerConfig` uses a hardcoded client secret (`{noop}secret`). `application.properties` relies on environment variables but defaults to mock values.
- **Risk:** Insecure defaults could accidentally be deployed to production.

## 5. Monolithic Scalability
- **Issue:** The Admin Dashboard (Thymeleaf/SSR) and the Storefront API (REST) run in the same JVM process (`modulith-service`).
- **Risk:** Heavy operations by an Admin (e.g., generating large support ticket reports or running bulk syncs) will degrade API response times for end-users shopping on the storefront.

## 6. Testing Infrastructure
- **Issue:** Testing relies on Docker (Testcontainers).
- **Risk:** As seen in development, environment issues (Docker availability, Java version) completely block the verification pipeline. A lighter-weight fallback strategy (like H2 for all tests, not just some) is partially implemented but inconsistent.
