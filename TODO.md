# Fashion Store Implementation Plan

## Project Goal
Build a modern, premium fashion e-commerce store using a headless architecture.

## Architecture & Stack
- **Frontend (UI)**: Next.js (Visuals, Client Logic)
- **Middleware**: Java Spring Boot (Business Logic, API Gateway, Adapter)
- **Headless Backend**: ERPNext (Inventory, Orders, Accounting, Master Data)

---

## Phase 1: PoC Integration (Foundation)
*Goal: Establish a functional data loop between all three systems.*

### 1.1 Infrastructure & Setup
- [x] **PostgreSQL**: Running (Container `postgres`).
- [x] **Java Middleware**: Running on port 8080.
    - [x] Fixed `specialPrice` logic.
    - [x] Added `getProductImage` endpoint.
    - [x] Added sync logging.
- [x] **Next.js Storefront**: Running on port 3000.
- [x] **ERPNext**:
    - [x] Deployed and running on port 8000.
    - [x] Site `frontend` (formerly `demo.local`) initialized and migrated.
    - [x] Confirmed assets are serving correctly.

### 1.2 Data Flow: Catalog (ERPNext -> Java -> Next.js)
- [ ] **ERPNext**: Create "Item" records (Fashion products with images/descriptions).
- [x] **Java**:
    - [x] Polished `ERPNextService` logic.
    - [x] Expose `/api/public/products` with transformed DTOs.
    - [x] Configured API Keys in `application.properties`.
    - [x] Verified connection: `>>> Found 0 items in ERPNext.` (Connection Successful).
- [ ] **Next.js**:
    - [ ] Verify `lib/java` adapter consumes the Java API.
    - [ ] Render products on the `ProductGrid`.

### 1.3 Data Flow: Orders (Next.js -> Java -> ERPNext)
- [ ] **Next.js**: Create a simple "Buy Now" or "Add to Cart" flow.
- [ ] **Java**:
    - [ ] Create `OrderService`.
    - [ ] Implement POST `/api/orders` endpoint.
    - [ ] Map incoming order to ERPNext `Sales Order` DocType.
- [ ] **ERPNext**: Verify Sales Order creation via API.

### 1.4 Verification
- [ ] **Live Sync Test**: Add item in ERPNext -> appears on Next.js frontend automatically (or via poll).
- [ ] **End-to-End Test**: User places order > Java processes it > Order visible in ERPNext.

---

## Phase 2: Feature Expansion (Development)
*Goal: Turn the PoC into a fully featured fashion store.*

### 2.1 Logic & Backend (Java)
- [ ] **Authentication**: Implement JWT Auth (sync users with ERPNext Customers?).
- [ ] **Cart Management**: Handle cart state (Redis or Database).
- [ ] **Search & Filter**: Implement search logic (by category, price, size) in Java (filtering cached data or querying ERPNext).
- [ ] **Inventory Sync**: Real-time stock checking before order placement.

### 2.2 Frontend Experience (Next.js)
- [ ] **Product Details**: Dedicated page (`/product/[slug]`) with rich media.
- [ ] **Cart UI**: Slide-out cart or dedicated page.
- [ ] **Checkout Flow**: Address input, Payment method selection.
- [ ] **User Profile**: Order history view.

### 2.3 Visuals & Polish
- [ ] **Premium Aesthetics**:
    - [ ] Typography: Use `Inter` or `Outfit` fonts.
    - [ ] Colors: Define a premium palette (e.g., Deep Charcoal, Gold/Beige accents).
    - [ ] Animations: Add `framer-motion` for page transitions and hover effects.
- [ ] **Responsive Design**: Ensure mobile-first excellence.

---

## Current Status
- **Completed**: End-to-end infrastructure is LIVE. Java and ERPNext are handshake-ready.
- **Immediate Next Step**: User to add items in ERPNext UI and verify storefront sync.
