# Deliverable List: Multi-Marketplace Integration

This document lists the specific artifacts and features to be delivered for the Multi-Marketplace Integration phase.

## 1. Marketplace Integration Service (MIS)
- [ ] **Project Structure**: Skeleton Spring Boot app with Eureka/Gateway integration.
- [ ] **Marketplace Adapters**:
    - [ ] `AmazonSPAPIAdapter`: OAuth2 flow and Order Ingestion.
    - [ ] `FlipkartAdapter`: Token-based authentication and Inventory Sync.
    - [ ] `MyntraAdapter`: Mapping for fashion-specific attributes (Size, Color).
- [ ] **Webhook Infrastructure**:
    - [ ] Signature Verification filters.
    - [ ] Idempotency Repository (Redis).
    - [ ] Webhook controller endpoints.
- [ ] **Normalization Engine**:
    - [ ] `OrderNormalizer` with Mapping utility.
    - [ ] Canonical `OrderDTO`.

## 2. Core Business Service (CBS)
- [ ] **ERP Integration**:
    - [ ] `ERPNextSyncService` for push/pull of Sales Orders.
    - [ ] `InventoryBroadcastService` to push stock updates to all active adapters.
- [ ] **Multi-Store Management**:
    - [ ] Database schema for `MarketplaceStore` credentials and settings.
    - [ ] Encryption layer for sensitive API keys.

## 3. Infrastructure & DevOps
- [ ] **Docker Configuration**: Updated `docker-compose.yml` with separate profiles for MIS and CBS.
- [ ] **Monitoring**: SkyWalking dashboards for marketplace order latency.
- [ ] **Rate Limiting**: Redis-based throttling configuration.

## 4. Documentation (Repository Docs)
- [x] `docs/HIGH_LEVEL_ARCHITECTURE.md` (Updated standard `ARCHITECTURE.md`)
- [x] `docs/MARKETPLACE_LLD.md` (New Low-Level Design)
- [ ] `docs/MARKETPLACE_ONBOARDING.md` (Guide for adding new marketplaces)
- [ ] `docs/DELIVERABLES.md` (This file)
