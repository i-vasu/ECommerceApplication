---
description: Fashion e‑Commerce PoC – Stabilized Multi-Tenant Production-Ready Flow
---

# Overview
This workflow provides the **stabilized, production-ready** setup for the fashion e‑commerce platform.

**Stack:**
- **ERPNext v15**: Core Business Engine (Inventory, Orders, Accounting).
- **Java Spring Boot**: Modular Monolith Middleware (Business Logic, Integrations).
- **ParadeDB**: PostgreSQL-based storage with advanced search capabilities.
- **Dragonfly**: High-performance Redis-compatible cache and queue.
- **Next.js Storefront**: Modern customer-facing UI.

---

## 1️⃣ Infrastructure Setup

### 1.1 Docker Network
Ensure a shared network exists for all containers.
```bash
docker network create ecommerce-net
```

### 1.2 Core Infrastructure (`docker-compose.yml`)
Includes ParadeDB (Postgres), Dragonfly (Redis), and Caddy (Reverse Proxy).
```yaml
# Use the existing docker-compose.yml in ECommerceApplication root
docker compose up -d
```

---

## 2️⃣ ERPNext Setup (Multi-Tenant)

### 2.1 ERPNext Services (`docker-compose.erpnext.yml`)
Uses external databases and caches.
```yaml
# Use the existing docker-compose.erpnext.yml in ECommerceApplication root
docker compose -f docker-compose.erpnext.yml up -d
```

### 2.2 Site Initialization Checklist
For every new tenant (e.g., `tenant1-test.localhost`):

1. **Create Site & Install App**:
   ```bash
   docker exec erpnext-backend bench new-site tenant1-test.localhost \
     --admin-password admin --db-root-password admin --install-app erpnext
   ```
2. **Generate API Keys**:
   ```bash
   # Generate for Administrator
   docker exec -u frappe erpnext-backend bench --site tenant1-test.localhost \
     execute frappe.core.doctype.user.user.generate_keys --args "['Administrator']"
   
   # Retrieve Key
   docker exec -u frappe erpnext-backend bench --site tenant1-test.localhost \
     execute frappe.db.get_value --args "['User', 'Administrator', 'api_key']"
   ```

3. **Update Middleware Database**:
   Update the `tenants` table in ParadeDB with the new `erpnextapikey` and `erpnextapisecret`.

---

## 3️⃣ Java Middleware Deployment

### 3.1 Build & Run
Ensure you target the specific module.
```bash
# From ECommerceApplication root
mvn spring-boot:run -pl modulith-service -DskipTests
```

### 3.2 Verification
Verify the sync endpoint for a tenant:
```bash
curl -I -H "Host: tenant1-test.localhost" \
  -H "Authorization: token KEY:SECRET" \
  http://localhost:8000/api/method/ping
```

---

## 4️⃣ Production Readiness Tips
- **Health Checks**: Always use the defined health checks in `docker-compose.yml`.
- **Memory Limits**: Respect the memory limits set in the compose files (optimized for 8GB/16GB environments).
- **Logging**: Check `logs/error.log` for backend issues and `docker logs` for infrastructure issues.