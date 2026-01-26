---
name: Marketplace Integration Plan
description: Plan for implementing the Marketplace Integration Service (MIS) normalization and dispatch logic.
---

# Feature: Marketplace Integration (Normalization & Dispatch)

## Objective
Implement the logic to normalize incoming order payloads from external marketplaces (Amazon, Flipkart) into the internal `OrderDTO` format, and successfully dispatch them to the Core Business Service (Order Service).

## Status
- [x] Webhook Controller (Basic)
- [x] Internal DTOs
- [x] Normalization Engine (Detailed Mapping)
- [x] Order Dispatch Service (Integration)
- [x] External DTOs (Refinement - Used Map Strategy)
- [ ] Unit Tests

## Detailed Steps

### 1. Define External DTOs
Refine `com.app.marketplace.dto.external` classes to model the incoming JSON structures from Amazon/Flipkart more accurately (or use generic Maps if schema varies too widely).

### 2. Implement Normalization Logic
Update `NormalizationEngine.java`:
- Implement `mapAmazonOrder` to extract fields like `AmazonOrderId`, `BuyerEmail`, `OrderTotal`, `ShippingAddress`.
- Implement `mapFlipkartOrder` to extract corresponding fields.
- Map distinct line items to `OrderItemDTO`.

### 3. Integrate Order Dispatch
Update `OrderDispatchService.java`:
- Use `NormalizationEngine` to convert raw payload -> `OrderDTO`.
- Ensure `placeMarketplaceOrder` in `OrderServiceImpl` is robust (handling missing user, creating ghost user if needed).

### 4. Testing
- Create `NormalizationEngineTest.java`.
- Create `OrderDispatchServiceTest.java`.
