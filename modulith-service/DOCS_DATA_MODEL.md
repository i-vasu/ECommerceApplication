# Data Model Documentation

This document describes the mapping between the Modulith Service database (PostgreSQL/ParadeDB), the Cache (DragonflyDB), and ERPNext.

## 1. Product Module (Fashion Optimized)

### PostgreSQL: `products` table
| Column | Type | ERPNext Mapping | Description |
|--------|------|-----------------|-------------|
| `product_id` | BIGINT (PK) | - | Internal unique ID |
| `product_name`| VARCHAR | `item_name` | Name of the product |
| `brand` | VARCHAR | `brand` | **Brand name (Universal Fashion Structure)** |
| `item_code` | VARCHAR | `item_code` (Unique) | ERPNext primary key for items |
| `description` | TEXT | `description` | Product details |
| `price` | DOUBLE | `standard_rate` | Selling price |
| `category_id` | BIGINT (FK) | `item_group` | Link to category |

### PostgreSQL: `product_variants` table (Size/Color Matrix)
| Column | Type | ERPNext Mapping | Description |
|--------|------|-----------------|-------------|
| `variant_id` | BIGINT (PK) | - | Internal unique ID |
| `item_code` | VARCHAR | `name` (Variant) | ERPNext item code for the variant |
| `product_id` | BIGINT (FK) | `variant_of` | Link to parent product |
| `size` | VARCHAR | `size` (Item Attribute)| **Size (e.g., XL, 42, 10)** |
| `color` | VARCHAR | `color` (Item Attribute)| **Color (e.g., Midnight Blue)** |
| `material` | VARCHAR | `material` (Attribute) | **Material (e.g., Organic Cotton)** |
| `stock_quantity`| INT | `actual_qty` (Bin) | Current stock level |
 
### PostgreSQL: `product_media` table (Gallery/Rich Media)
| Column | Type | ERPNext Mapping | Description |
|--------|------|-----------------|-------------|
| `media_id` | BIGINT (PK) | - | Internal unique ID |
| `product_id` | BIGINT (FK) | `attached_to_name` (File)| Link to parent product |
| `type` | VARCHAR | - | IMAGE or VIDEO |
| `url` | VARCHAR | `file_url` (File) | Full CDN or relative URL |
| `display_order`| INT | - | Ordering in the gallery |
| `blur_hash` | VARCHAR | - | Lazy-loading placeholder |

### DragonflyDB (Redis): Stock Cache
| Key | Value Type | Description |
|-----|------------|-------------|
| `inventory:stock:{item_code}` | String (Integer)| Real-time stock level for fast checkout validation |

---

## 2. Order Module (Multi-Marketplace Support)

### PostgreSQL: `orders` table
| Column | Type | ERPNext Mapping | Description |
|--------|------|-----------------|-------------|
| `order_id` | BIGINT (PK) | `po_no` | Site-specific Order ID |
| `erpnext_order_name` | VARCHAR | `name` (Sales Order)| ERPNext Sales Order ID |
| `marketplace_source` | VARCHAR | `source` (Custom) | **Source (e.g., Amazon, Shopify, Web Store)** |
| `marketplace_order_id`| VARCHAR | `market_order_id`| **External ID from the marketplace** |
| `email` | VARCHAR | `customer` | Customer identifier |
| `total_amount`| DOUBLE | `grand_total` | Total order value |
| `order_status`| VARCHAR | `status` | Local status (mapped from ERPNext) |

---

## 3. Identity Module

### PostgreSQL: `users` table
| Column | Type | ERPNext Mapping | Description |
|--------|------|-----------------|-------------|
| `user_id` | BIGINT (PK) | - | Internal unique ID |
| `email` | VARCHAR | `email_id` | Primary identifier |
| `first_name` | VARCHAR | `customer_name` (part) | |
| `last_name` | VARCHAR | `customer_name` (part) | |

---

---

## 4. Multi-Tenancy Strategy (Site + ERPNext)

### Application Level (Modulith Service)
- **Schema-Based Multi-Tenancy**: Each tenant/brand has its own PostgreSQL schema (e.g., `brand_a.products`, `brand_b.products`).
- **Tenant Context**: Determined via `X-Tenant-ID` header or custom domain mapping.

### ERPNext Level
- **Frappe Multi-Tenancy**: We utilize Frappe's native multi-site setup. Each tenant maps to a unique ERPNext Site (e.g., `brand-a.erpnext.local`).
- **Data Isolation**: Ensures complete separation of inventory, accounting, and customers between different fashion labels managed on the same infrastructure.

---

## 5. Universal Fashion Store Standards (DocTypes)

Our structure follows the **GS1 Global Standards** and industry best practices for Apparel & Retail:

### Product Intelligence
- **Variant Matrix**: Uses `Item Attribute` (Size, Color, Fit, Fabric) to create a lean hierarchy. A "T-Shirt" is a 'Template', and "Red-XL" is the 'Variant'.
- **Seasonality**: Custom fields on `Item` for `Season` (e.g., AW25, SS24) and `Collection`.
- **Brand Management**: Centralized `Brand` DocType linked to all items for cross-catalog filtering.
- **Rich Media**: Dedicated `Product Media` table for 360-degree views, video lookbooks, and zoomable fabric textures.

### Order Life Cycle
- **Omnichannel Support**: `marketplace_source` allows tracking if an order came from the Mobile App, Web Store, or a 3rd party like Amazon.
- **Fashion-Specific Statuses**: Support for `Awaiting Alteration`, `Ready for Pickup`, and `Pre-Order` workflows.

---

## Production-Ready Strategy
- **Source of Truth**: ERPNext remains the absolute master for Master Data (Items, Stock, Pricing).
- **High-Performance Read Model**: Modulith Service (ParadeDB/Dragonfly) acts as a high-speed "Projection" for the frontend.
- **Real-time Sync**: Bi-directional sync via Webhooks (ERPNext -> Java) and REST API (Java -> ERPNext for Sales Orders).
- **Global Compliance**: GDPR/CCPA ready with isolated tenant schemas.
 
---
 
### FAQ: Sync Inconsistency & Location
**Q: Where are the tables for variants and media?**
- **Variants**: `product_variants` (ParadeDB) & `Item` (ERPNext - variants have `variant_of` set).
- **Media**: `product_media` (ParadeDB) & `File` (ERPNext - linked to `Item`).
 
**Q: Are they in sync?**
- **Yes.** The `ProductDataFlowService` handles a multi-step sync:
  1. `Item Group` -> `categories`
  2. `Item` (Template) -> `products`
  3. `Item` (Variant) -> `product_variants`
  4. `File` (Attached to Item) -> `product_media`
  5. `Image` -> `Search Embeddings` (Vector Data)
- **Automatic Triggers**: Regular intervals via `SyncScheduler` and real-time updates via `ERPNextWebhookController`.
 
---
 
### 6. Production Readiness: Essential Fashion DocTypes
To be truly production-ready, the following DocTypes must be configured/extended in ERPNext:
 
| Category | DocType | Status | Purpose |
|----------|---------|--------|---------|
| **Catalog** | `Item Attribute` | Required | Manages Size/Color/Fabric variants |
| **Catalog** | `Brand` | Required | Global brand management & filtering |
| **Catalog** | `Season` (Custom) | Recommended | Links items to Seasonal Collections (SS24/AW25) |
| **Catalog** | `Size Guide` (Custom)| Recommended | Maps internal sizes to body measurements |
| **Inventory**| `Price List` | Required | Marketplace-specific pricing (e.g., App-only deals) |
| **Inventory**| `Stock Selection` | Required | First-In-First-Out (FIFO) for expiring fashions |
| **Logistics**| `Shipping Rule` | Required | weight/pincode based rate calculations |
| **Compliance**| `Tax Rule` | Required | GST/VAT/Sales Tax localized calculations |
| **Marketing**| `Loyalty Program` | Required | Core of the Reward/Points system |
| **Marketing**| `Campaign` | Required | Tracking conversion from specific ads/emails |
| **Returns** | `Sales Return` | Critical | Automated return logistics & quality checks |
 
### FAQ

#### What DocTypes in ERPNext for a fashion store?
Essential DocTypes include `Item Attribute` (Size, Color, Material), `Brand`, `Price List`, `Tax Rule`, `Sales Return`, and `Voucher`.

#### Are ERPNext, ParadeDB, and DragonflyDB in sync?
Yes.
1. **ParadeDB (Postgres)**: Managed via JPA/Hibernate in the Modulith Service. Tables are automatically created/updated on startup using `spring.jpa.hibernate.ddl-auto: update`.
2. **DragonflyDB (Redis)**: Cache is warmed by the `ERPNextProductSyncService` during the synchronization process (`syncStock()` method).
3. **ERPNext**: Serves as the Master Data source. DocTypes must be configured in ERPNext first; the Modulith Service then pulls and synchronizes this data into Postgres and Redis.

To ensure they are in sync, run the **Synchronize All** task from the Admin Dashboard, which triggers the `ERPNextProductSyncService` flow.

Create 2 tenants in ERPNext, each tenent should have test,prod. for each of them must have all the doctypes mentioned in this file. Once Done keep paradeDB,DragonFlyDB in sync with ERPNext.