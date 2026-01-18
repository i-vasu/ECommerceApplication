# E-Commerce Microservices Architecture Documentation

This document provides a high-level overview of the system architecture, component interactions, and core business flows.

## 1. High-Level Architecture (UML Deployment Diagram)

The system follows a microservices architecture pattern, utilizing Spring Cloud for infrastructure concerns and independent services for business domains.

### Core Components:
- **API Gateway**: Entry point for all client requests. Handles routing and security.
- **Discovery Server (Eureka)**: Service registry for dynamic service discovery.
- **Product Service**: Manages catalog, categories, and inventory. Integrates with ERPNext for master data sync.
- **Order Service**: Manages shopping carts and order lifecycle. Integrates with Razorpay for payments and ERPNext for order fulfillment sync.
- **Persistence**: Separate PostgreSQL instances for services (logical/physical separation).
- **Cache**: Redis for product catalog caching and session management.

![System Architecture](../high_level_architecture.png)

---

## 2. Core Scenarios & Flows

### 2.1 Checkout Process (UML Sequence Diagram)

The checkout process involves inter-service communication to ensure consistency between orders and product data.

![Checkout Flow](../order_checkout_flow.png)

1. **Cart Creation**: User adds items to cart in `Order Service`.
2. **Product Validation**: `Order Service` fetches latest prices/stock via `ProductClient` (Feign).
3. **Order Finalization**: User submits order.
4. **Payment**: Integration with Razorpay.
5. **Fulfillment Sync**: Completed order is pushed to `ERPNext`.

### 2.2 ERPNext Product Synchronization

The `Product Service` acts as the downstream consumer for master data managed in ERPNext.

![Sync Flow](../erpnext_sync_flow.png)

1. **Sync Trigger**: Admin triggers manually or automated polling/webhook.
2. **Data Fetch**: `ERPNextProductSyncService` calls ERPNext Item APIs.
3. **Local Persistence**: Items are saved/updated in the local PostgreSQL.

---

## 3. Technology Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.4.1, Spring Cloud |
| Security | Keycloak (OIDC), OAuth2 Resource Server |
| Database | PostgreSQL 15 |
| Cache | Redis |
| Observability | Prometheus, Grafana |
| Integration | Feign, REST, ERPNext, Razorpay |
