# Project Context & Agent Instructions

This repository is part of a **Fashion Store** platform with a headless architecture.

## Architecture
1. **ERPNext (Platform)**: Located at `../erpnext-platform/`. Handles all master data (Items, Customers, Inventory).
2. **Java Middleware (Backend)**: Current directory. Processes business logic and acts as an adapter for ERPNext.
3. **Next.js Storefront (Frontend)**: Located at `../vaabhi-storefront/`. The customer-facing UI.

## Working with this project
- **ERPNext Integration**: Logic is in `ERPNextService.java`. It polls ERPNext every 60s.
- **Port Mapping**:
  - ERPNext: 8000
  - Java: 8080
  - Next.js: 3000
- **Git Strategy**: Follow `.agent/workflows/branching_strategy.md`. Use `develop` for daily work.
- **CI/CD**: GitHub Actions are configured in `.github/workflows/`.

## Important Files
- `TODO.md`: Current project roadmap and status.
- `src/main/resources/application.properties`: Contains ERPNext API keys and DB config.
- `.agent/workflows/fashion_ecommerce_poc.md`: Original setup instructions.
