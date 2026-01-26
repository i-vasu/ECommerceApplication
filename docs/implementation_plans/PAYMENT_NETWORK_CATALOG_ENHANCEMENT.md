# Implementation Plan: Payments, Network Discovery, and Smart Cataloging

This plan outlines the parallel implementation of **Juspay Hyperswitch**, **Beckn Protocol**, and **Real-time AI Cataloging** into the Universal E-Commerce Platform.

## 1. Hyperswitch Integration (Payments)
**Goal:** Replace/augment Razorpay with a unified payment orchestrator.

### Backend (`modulith-order`)
- [ ] Add Hyperswitch Java SDK dependency.
- [ ] Create `com.app.payment.services.HyperswitchService`.
- [ ] Implement `PaymentService` interface to unify Razorpay and Hyperswitch.
- [ ] Add `HyperswitchWebhookController` for async payment status updates.
- [ ] Configuration: Add `hyperswitch.api.key` and `hyperswitch.merchant.id` to `application.properties`.

### Frontend (`vaabhi-storefront`)
- [ ] Create `components/checkout/HyperswitchPayment.tsx` using Hyperswitch Web SDK.
- [ ] Update checkout flow to toggle between direct Razorpay and Hyperswitch.

## 2. Beckn Protocol Integration (ONDC Discovery)
**Goal:** Enable your platform to act as a Beckn Provider Platform (BPP).

### Backend (`modulith-order` & `modulith-product`)
- [ ] **Discovery (`/search`)**: 
    - [ ] Create `BecknDiscoveryController`.
    - [ ] Map Beckn `search` intent to `ParadeDB` vector/text search.
- [ ] **Transaction (`/select`, `/init`, `/confirm`)**:
    - [ ] Map Beckn DTOs to internal `OrderDTO`.
    - [ ] Reuse `OptimizedCheckoutService` for Beckn-originated orders.
- [ ] **Fulfillment (`/status`, `/track`)**:
    - [ ] Map internal shipment states (Shiprocket) to Beckn status codes.

## 3. Smart Cataloging & Real-time Sync (Product)
**Goal:** Move from polling-only sync to real-time, AI-enriched cataloging.

### Backend (`modulith-service`)
- [ ] **ERPNext Webhooks**:
    - [ ] Implement `ERPNextWebhookController` to listen for `Item` and `Bin` updates.
    - [ ] Immediate cache invalidation in DragonflyDB on update.
- [ ] **AI Enrichment (`modulith-product`)**:
    - [ ] Hook into the sync flow to trigger DJL (Deep Java Library) for automatic image tagging.
    - [ ] Populate `ParadeDB` with AI-generated semantic tags.
- [ ] **Asset Pipeline**:
    - [ ] Implement `AssetInjestor` to move ERPNext images to a high-speed CDN endpoint.

## 4. Path C: Service Bundling & Strategic Network Capabilities (Beckn Advanced)
**Goal:** Transform into a Unified Fashion Node.

- [ ] **Security (`BecknSigningService`)**:
    - [ ] Implement Ed25519 request signing for all outgoing Beckn messages (L1/L2).
- [ ] **Hyper-Local Search**:
    - [ ] Integrate geo-spatial filtering in `BecknDiscoveryService` using ParadeDB.
- [ ] **Service Orchestration (`BecknNetworkSeeker`)**:
    - [ ] Broadcast `/search` for Tailoring BPPs when unstitched/custom items are purchased.
    - [ ] Broadcast `/search` for Logistics BPPs to automate delivery fulfillment.
- [ ] **AI Recommendation Logic**:
    - [ ] Map Beckn high-level intents (e.g., "vibey wear") to AI-generated semantic tags.

## 4. Execution Phases
1. **Infrastructure**: Setup Hyperswitch keys and ERPNext webhook secrets.
2. **Contract Definition**: Define Beckn-compliant API endpoints.
3. **Core Logic**: Implement the services in their respective modules.
4. **Integration Testing**: Verify the end-to-end flow from Beckn search to Hyperswitch payment.
