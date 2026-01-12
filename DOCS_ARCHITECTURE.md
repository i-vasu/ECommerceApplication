# E-Commerce Platform Architecture & Flow Diagrams

This document outlines the decoupled, high-performance architecture designed for 100+ RPS.

---

## 🏗️ 1. System Component Architecture
```mermaid
graph TD
    subgraph "Public Internet"
        UI[Next.js Storefront]
    end

    subgraph "Application Layer (Stateless)"
        Java[Java Middleware]
        SW[SkyWalking Agent]
        Java --- SW
    end

    subgraph "Management Layer"
        ERP[ERPNext Core]
    end

    subgraph "External Data Layer (Decoupled - /ecommerce-data)"
        DB[(PostgreSQL)]
        MinIO[(MinIO Object Storage)]
        Redis[(Redis Cache)]
        Banyan[(BanyanDB O11y)]
    end

    UI -->|API Requests| Java
    Java -->|Cache Check| Redis
    Java -->|Persistence| DB
    Java -->|Async Sync| ERP
    ERP -->|Mirror Media| MinIO
    Java -->|Pre-signed URLs| MinIO
    SW -->|Traces| Banyan
```

---

## 🔄 2. Product Synchronization Flow (ERPNext -> MinIO)
```mermaid
sequenceDiagram
    participant E as ERPNext
    participant J as Java Middleware
    participant M as MinIO
    participant P as PostgreSQL
    participant R as Redis

    Note over J: Scheduled Task (Every 60s)
    J->>E: GET /api/resource/Item
    E-->>J: JSON (Item + Image URL)
    
    loop For each New/Updated Item
        J->>E: Download Full-Res Image
        E-->>J: Binary Bytes
        J->>M: Upload as Object Key
        M-->>J: OK (prod_123.jpg)
        J->>P: INSERT/UPDATE Item (MinIO Key)
        P-->>J: OK
    end

    J->>R: EVICT "products_*"
```

---

## ⚡ 3. High-Load Serving Flow (Cached)
```mermaid
sequenceDiagram
    participant C as Customer (Browser)
    participant J as Java Middleware
    participant R as Redis
    participant P as PostgreSQL
    participant M as MinIO

    C->>J: GET /api/products
    J->>R: Check Key "products_page_1"
    
    alt Cache Hit
        R-->>J: Cached JSON Product List
    else Cache Miss
        J->>P: SELECT * FROM products
        P-->>J: Result Set
        J->>R: SET Key "products_page_1"
    end

    loop For each Product in list
        J->>J: Generate Pre-signed URL (MinIO Key)
    end

    J-->>C: JSON Response (w/ High-Speed Media Links)
```
