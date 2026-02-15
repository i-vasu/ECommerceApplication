---
description: Fashion e‑Commerce PoC – Stabilized Multi-Tenant Production-Ready Flow
---

# 🛍️ Vaabhi Heritage Fashion Platform

Welcome to the **stabilized, production-ready** architectural guide for the Vaabhi Heritage Fashion platform. This repository implements a state-of-the-art **Modular Monolith** designed for extreme scalability, maintainability, and multi-tenant isolation.

---

## 🏛️ Architectural Overview

The platform is built on a high-performance stack that prioritizes modern Java features and cloud-native observability:

- **Core Engine**: Java 25 + Spring Boot 4.0.2 (using Virtual Threads & ScopedValues).
- **Architecture**: [Spring Modulith](https://spring.io/projects/spring-modulith) for domain-driven isolation within a single deployment unit.
- **Storage**: [ParadeDB](https://www.paradedb.com/) (PostgreSQL 16+) with native BM25 search and analytical extensions.
- **Caching & Messaging**: [DragonflyDB](https://dragonflydb.io/) – the modern, multi-threaded alternative to Redis.
- **Admin & Ops**: [Vaadin 25](https://vaadin.com/) for a seamless, state-of-the-art internal management interface.
- **Storefront**: [Next.js](https://nextjs.org/) for a lightning-fast customer-facing experience.
- **Observability**: Full LGTM stack (Loki, Grafana, Tempo, Melloy/Alloy).

---

## 🚀 Getting Started

### 1️⃣ Prerequisites
- **JDK 25**: Required for `ScopedValue` and preview features.
- **Podman & Podman-compose**: For rootless container orchestration.
- **Maven 3.9+**: For building the backend.

### 2️⃣ Infrastructure Deployment
Initialize the shared network and start the core infrastructure components:

```powershell
# Create the shared bridge network
podman network create ecommerce-net

# Start the core data & observability services
podman-compose up -d
```

| Service | Port | Description |
| :--- | :--- | :--- |
| **ParadeDB** | `5432` | Primary PostgreSQL Instance |
| **Dragonfly** | `6379` | High-Performance Cache |
| **Caddy** | `80/443`| Reverse Proxy & TLS |
| **Grafana** | `3000` | Analytics & Monitoring |
| **Prometheus**| `9090` | Metrics Storage |

### 3️⃣ Backend Development Workflow

The backend is organized into domain-specific modules. The `modulith-service` module is the consolidated entry point.

#### Build the Project
```bash
mvn clean install -DskipTests
```

#### Run the Application
Always run with `--enable-preview` to support Java 25's modern concurrency and scoping models:

```bash
mvn spring-boot:run -pl modulith-service -Dspring-boot.run.jvmArguments="--enable-preview -XX:+UseZGC -Xmx2g"
```

---

## 🏢 Multi-Tenancy Strategy

The platform uses a sophisticated **Shared Database / Shared Schema** model with **Dynamic Tenant Context Propagation**:

1.  **Context**: Managed via `TenantContext` using Java 25 `ScopedValue`.
2.  **Filter**: `TenantFilter` extracts the `X-Tenant-ID` header or `TENANT_ID` cookie.
3.  **Isolation**: The kernel ensures that all repository queries are tenant-aware (discriminator based) without manual boilerplate.

### Provisioning a Tenant
To add a new tenant, simply insert a record into the `public.tenants` table:
```sql
INSERT INTO public.tenants (tenant_id, name, environment, active)
VALUES ('vaabhi_india', 'Vaabhi Heritage India', 'PROD', true);
```

---

## 🛠️ Internal Operations (Vaadin Admin)

The administrative suite is accessible at `/admin`. It provides a "Premium ERP" experience including:
- **Procurement Command Center**: Manage vendors and Purchase Orders (modulith-logistics/support).
- **Inventory Governance**: Real-time stock reconciliation and transaction logs.
- **Operational Dashboard**: Technical health monitoring, log search, and distributed tracing visualization.

---

## 🔍 Verification & Health

Check the status of your deployment via the following endpoints:

- **Global Health**: `http://localhost:8080/actuator/health`
- **Tenant API Verification**:
  ```bash
  curl -H "X-Tenant-ID: vaabhi_india" http://localhost:8080/api/v1/catalog/products
  ```
- **API Documentation**: `http://localhost:8080/swagger-ui.html`

---

## 🛡️ Security Model
- **API (Stateless)**: JWT-based authentication via `JWTFilter`. All tokens carry a `tenantId` claim for cross-tenant validation.
- **Admin (Stateful)**: Form-based login for administrative users with Role-Based Access Control (RBAC).

---

## 📈 Roadmap & Next Steps
- [ ] Integration with Indian GST compliance (modulith-finance).
- [ ] Advanced Warehouse Management (modulith-logistics).
- [ ] AI-driven Visual Search (modulith-intelligence).
