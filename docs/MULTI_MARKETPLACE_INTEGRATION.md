# Multi-Marketplace Order Integration System

## 🎯 Overview
The Multi-Marketplace Order Integration System is a comprehensive platform designed to centralize order processing from multiple e-commerce channels (Amazon, Flipkart, Myntra, etc.) and forward them to ERPNext for fulfillment.

## 🏗️ Architecture (UML Component Diagram)
The system uses a **Consolidated Microservices Architecture** optimized for a 4.5GB memory constraint while maintaining high extensibility.

![Multi-Marketplace Architecture](multi_marketplace_uml_architecture)

### Supported Channels:
- **Major Marketplaces**: Amazon (SP-API), Flipkart, Nykaa, Myntra, Ajio.
- **Open Networks**: ONDC.
- **Direct**: Custom Website Integration.

---

## 🚦 Core Business Flow (UML Sequence Diagram)

The system follows a normalized data pipeline to ensure consistency across disparate marketplace schemas.

![Order Normalization Flow](order_normalization_seq_uml)

### Process Steps:
1. **Ingestion**: Webhook or Polling adaptors receive the raw order payload.
2. **Normalization**: The `Marketplace Integration Service` transforms the payload into a standard `OrderDTO`.
3. **Processing**: The `Core Business Service` validates the order and checks inventory.
4. **ERP Sync**: The normalized order is pushed to ERPNext as a Sales Order.
5. **Acknowledge**: Success status is returned to the source marketplace.

---

## 🚀 Key Features

### 1. Unified Order Normalization
Every incoming order, regardless of source (Amazon JSON, Flipkart XML, etc.), is mapped to a singular internal schema. This prevents "code bloat" in the core business logic.

### 2. Multi-Store / Multi-Tenant Support
The system supports multiple accounts for the same marketplace (e.g., managing two separate Amazon seller accounts) with isolated credentials and warehouse mappings.

### 3. Inventory & Pricing Sync
- **Real-time Sync**: ERPNext acts as the source of truth; stock levels are broadcasted to all marketplaces upon change.
- **Marketplace Rules**: Support for channel-specific pricing (e.g., different discounts for Ajio vs. Myntra).

---

## 💾 Memory Optimization
- **API Gateway + Registry**: 450MB
- **Marketplace Integration Service**: 900MB (Handles all adapters)
- **Core Business Service**: 750MB (ERP & Logic)
- **Total Footprint**: ~2.1GB (Excluding shared infrastructure like Postgres/Redis)

---

## 📝 Development Workflow: Adding a New Marketplace
1. Create a new adapter in `marketplace-integration-service/adapters/`.
2. Implement the `MarketplaceAdapter` interface.
3. Define the normalization mapping in the `Normalizer` component.
4. Add credentials to the encrypted vault/application properties.
5. Enable the feature flag for the new channel.
